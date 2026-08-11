package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_LIGHT_USER_DISABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.USER_BLOCK
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.USER_DELETE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.USER_ROLE_UPDATE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.USER_UNBLOCK
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.IUserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID

@Service
class UserService(
	private val port: IUserPort,
	private val preferencesService: IPreferencesService,
	private val userProjectProfileService: IUserProjectProfileService,
	private val transactionalOperator: TransactionalOperator,
	private val roleService: IRoleService,
	private val auditService: IAuditService,
	@param:Value($$"${registry.feature.light-user.enabled:true}")
	private val lightUserEnabled: Boolean,
) : ApplicationListener<ContextRefreshedEvent>, IUserService, GenericService() {
	private lateinit var serviceAccount: CurrentUserModel

	override fun onApplicationEvent(event: ContextRefreshedEvent) {
		port.findServiceAccount()
			.subscribe { serviceAccount = it }
	}

	override fun findUsersPage(
		pageable: PageableModel,
		searchParams: UserSearchParamModel,
		sort: List<SortModel<UserSortFieldEnum>>,
	): Mono<PageModel<UserModel>> {
		return port.findPage(pageable, searchParams, sort)
	}

	override fun findUserById(id: UUID, visibilitySearched: Boolean?): Mono<UserModel> {
		return port.findById(id, visibilitySearched)
			.filter { isNotServiceAccount(it) }
			.notFoundIfEmpty(id)
	}

	private fun isNotServiceAccount(user: UserModel): Boolean = user.id != serviceAccount.id

	override fun findUserByOidcId(id: UUID, visibilitySearched: Boolean?): Mono<CurrentUserModel> {
		return port.findByOidcId(id, visibilitySearched)
			.filter { isNotServiceAccount(it) }
	}

	override fun findUserByEmail(email: String, visibilitySearched: Boolean?): Flux<CurrentUserModel> {
		return port.findByEmail(email, visibilitySearched)
			.filter { isNotServiceAccount(it) }
	}

	override fun serviceAccount(): UserModel = serviceAccount

	override fun assignableUserRoles(currentUser: CurrentUserModel): Flux<String> {
		return Flux.fromIterable(roleService.getAssignableUserRoles(currentUser))
	}

	override fun createUser(
		oidcId: UUID,
		email: String,
		firstName: String?,
		lastName: String?
	): Mono<CurrentUserModel> {
		log.info("Saving new user (OIDC ID \"{}\") in database", oidcId)
		val user = UserModel(
			oidcId = oidcId,
			email = email,
			firstName = firstName,
			lastName = lastName,
			role = roleService.getDefaultUserRole(),
			lastLogin = ZonedDateTime.now()
		)
		user.create(serviceAccount)

		return port.create(user)
			.map { CurrentUserModel(it) }
			.flatMap { createdUser ->
				preferencesService.findByUser(createdUser)
					.map {
						createdUser.preferences = it
						createdUser
					}
			}
			.`as`(transactionalOperator::transactional)
	}

	/**
	 * First-login linking of an "invited" (email-only) account: binds the IdP
	 * `oidcId` to an existing user matched by email, refreshes personal data and
	 * `lastLogin`, and KEEPS the existing role. Preferences are already loaded by
	 * the by-email query, so no reload is needed; a single update, no transaction.
	 */
	override fun linkUser(
		user: CurrentUserModel,
		oidcId: UUID,
		email: String,
		firstName: String?,
		lastName: String?
	): Mono<CurrentUserModel> {
		log.info("Linking OIDC ID \"{}\" to existing invited user \"{}\"", oidcId, user.id)
		user.oidcId = oidcId
		user.email = email
		user.firstName = firstName
		user.lastName = lastName
		user.lastLogin = ZonedDateTime.now()

		return updateUser(serviceAccount, user)
			.map { user }
	}

	/**
	 * Gated by `registry.feature.light-user.enabled`: with the feature off no
	 * email-only account may be minted, so an email nobody holds is refused
	 * rather than silently turned into an invitation that first-login linking
	 * would never claim.
	 */
	override fun findOrCreateInvitedUser(email: String, inviter: CurrentUserModel): Mono<UserModel> {
		return port.findByEmail(email, visibilitySearched = null)
			.filter { isNotServiceAccount(it) }
			.next()
			.map<UserModel> { it }
			.switchIfEmpty(Mono.defer {
				if (lightUserEnabled) {
					createInvitedUser(email, inviter)
				} else {
					log.warn("The light user feature is disabled, refusing to invite the unknown address")
					Mono.error(
						RegistryException(
							UNPROCESSABLE_ENTITY,
							PROJECT_PROFILE_LIGHT_USER_DISABLED,
							arrayListOf(email)
						)
					)
				}
			})
	}

	/**
	 * Feature-flip fallback for `registry.feature.light-user.enabled=false`: drop
	 * an unclaimed email-only invitation so the sign-in that found it can
	 * self-register instead of stalling on an account nobody can claim. Deleting
	 * cascades to its still-pending project profiles, exactly as the light-user
	 * purge does. Defensive on both fields — an account already bound to an IdP
	 * identity is never a light user and must survive.
	 */
	override fun deleteLightUser(user: UserModel): Mono<Unit> {
		val id = user.id
		if (id == null || user.oidcId != null) {
			return Mono.empty()
		}
		log.info("The light user feature is disabled, deleting the residual light user \"{}\"", id)
		return port.deleteById(id)
	}

	/**
	 * Creates an email-only user (no `oidcId`, no names) so someone can be added
	 * to a project before ever logging in. `lastLogin` stays null on purpose: the
	 * purge query filters on `last_login`, so never-logged-in invitees are never
	 * auto-purged. The IdP identity is bound later on first login via [linkUser].
	 */
	private fun createInvitedUser(email: String, inviter: CurrentUserModel): Mono<UserModel> {
		log.info("Creating an invited (email-only) user on behalf of \"{}\"", inviter.id)
		val user = UserModel(
			oidcId = null,
			email = email,
			firstName = null,
			lastName = null,
			role = roleService.getDefaultUserRole(),
			lastLogin = null,
		)
		user.create(inviter)

		return port.create(user)
	}

	override fun updateUserIfPersonalDataChanged(
		user: CurrentUserModel,
		email: String,
		firstName: String?,
		lastName: String?
	): Mono<CurrentUserModel> {
		if (user.personalDataChanged(email, firstName, lastName)) {
			log.info("Updating personal data for user \"{}\"", user.id)
			user.email = email
			user.firstName = firstName
			user.lastName = lastName
		}

		val now: ZonedDateTime = ZonedDateTime.now()
		log.info("Updating user \"{}\" last sign in date and time to {}", user.id, now)
		user.lastLogin = now

		return updateUser(serviceAccount, user)
			.map { user }
	}

	private fun updateUser(currentUser: CurrentUserModel, user: UserModel): Mono<UserModel> {
		return port.update(user.apply { update(currentUser) })
	}

	override fun updateUserRoleById(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserModel> {
		val assignableRoles = roleService.getAssignableUserRoles(currentUser)
		val updated = findUserByIdWithEligibleRole(assignableRoles, id, visibilitySearched = true)
			.validateRole(currentUser, assignableRoles, role, USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN)
			.validateNotLastRoleLevel0(USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE)
			.flatMap {
				it.role = role
				updateUser(currentUser, it)
			}
		return auditService.audit(updated, currentUser, USER_ROLE_UPDATE, id)
	}

	private fun findUserByIdWithEligibleRole(
		assignableRoles: List<String>,
		id: UUID,
		visibilitySearched: Boolean?
	): Mono<UserModel> {
		return findUserById(id, visibilitySearched)
			.filter { Objects.isNull(it.role) || assignableRoles.contains(it.role) }
			.notFoundIfEmpty(id)
	}

	override fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
		val allowedRoles = roleService.getAssignableUserRoles(currentUser)
		val blocked = findUserByIdWithEligibleRole(allowedRoles, id, visibilitySearched = true)
			.validateNotCurrentUser(currentUser, USER_BLOCK_CURRENT_USER)
			.validateNotLastRoleLevel0(USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR)
			.validateNotLastProjectRoleLevel0(USER_BLOCK_LAST_PROJECT_ADMINISTRATOR)
			.updateVisibility(visibility = false)
			.flatMap { updateUser(currentUser, it) }
		return auditService.audit(blocked, currentUser, USER_BLOCK, id)
	}

	override fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
		val allowedRoles = roleService.getAssignableUserRoles(currentUser)
		val unblocked = findUserByIdWithEligibleRole(allowedRoles, id, visibilitySearched = false)
			.updateVisibility(visibility = true)
			.flatMap { updateUser(currentUser, it) }
		return auditService.audit(unblocked, currentUser, USER_UNBLOCK, id)
	}

	/**
	 * Erasure is a real deletion, not a scrubbed shadow row: memberships and
	 * preferences go with the account (FK cascade), while the records it created
	 * survive with their author reference cleared (FK set null). Who did what
	 * stays in the append-only audit trail, which lives outside the database.
	 */
	override fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		val allowedRoles = roleService.getAssignableUserRoles(currentUser)
		val deleted = findUserByIdWithEligibleRole(allowedRoles, id, visibilitySearched = null)
			.validateNotCurrentUser(currentUser, USER_DELETE_CURRENT_USER)
			.validateNotLastRoleLevel0(USER_DELETE_LAST_APPLICATION_ADMINISTRATOR)
			.validateNotLastProjectRoleLevel0(USER_DELETE_LAST_PROJECT_ADMINISTRATOR)
			.flatMap { port.deleteById(it.id!!) }
		return auditService.audit(deleted, currentUser, USER_DELETE, id)
	}

	/**
	 * Self-service erasure: the same pipeline as [deleteUserById] minus the
	 * not-current-user guard, which would otherwise always reject the caller
	 * erasing their own account. Both last-administrator guards still apply, so
	 * nobody deletes themselves out of a responsibility they alone hold.
	 */
	override fun deleteCurrentUser(currentUser: CurrentUserModel): Mono<Unit> {
		val allowedRoles = roleService.getAssignableUserRoles(currentUser)
		val deleted = findUserByIdWithEligibleRole(allowedRoles, currentUser.id!!, visibilitySearched = null)
			.validateNotLastRoleLevel0(USER_DELETE_LAST_APPLICATION_ADMINISTRATOR)
			.validateNotLastProjectRoleLevel0(USER_DELETE_LAST_PROJECT_ADMINISTRATOR)
			.flatMap { port.deleteById(it.id!!) }
		return auditService.audit(deleted, currentUser, USER_DELETE, currentUser.id!!)
	}

	override fun purgeUsersIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging users inactive since {}", dateThreshold)
		return purge(port.findUserIdsOlderThanLastLogin(dateThreshold), dryRun)
	}

	/**
	 * Purges stale light users — email-only invitations (no oidc_id) never claimed by a
	 * first login — by their creation age. They are exempt from [purgeUsersIfNecessary]
	 * (which keys on last-login, always null here). Deleting cascades to their pending
	 * project profiles (FK ON DELETE CASCADE).
	 */
	override fun purgeLightUsersIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging light users (unclaimed invitations) created before {}", dateThreshold)
		return purge(port.findLightUserIdsOlderThanCreation(dateThreshold), dryRun)
	}

	private fun purge(userIds: Flux<UUID>, dryRun: Boolean): Flux<UUID> {
		return userIds.flatMap {
			if (dryRun) {
				log.info("[Dry run] user {} would be deleted", it)
				Mono.just(it)
			} else {
				log.info("Purging user {}", it)
				port.deleteById(it).thenReturn(it)
					.doOnNext { e -> log.info("User {} was deleted", e) }
					.doOnError { err -> log.error("Failed to purge user {}", it, err) }
			}
		}
	}

	/**
	 * Structural equality, never `===`: both sides are distinct UUID instances
	 * loaded from different places, so referential equality is false even for the
	 * same account. With `===` this guard never fired, and the two actions it
	 * protects — block and delete — both accepted the caller's own row, though
	 * neither can be walked back: a deleted account is gone, and a blocked one
	 * cannot even call unblock on itself.
	 */
	private fun Mono<UserModel>.validateNotCurrentUser(currentUser: CurrentUserModel, error: String) =
		handle { it, handle ->
			if (it.id == currentUser.id) {
				log.warn("The user {} is the current user", currentUser.id)
				handle.error(RegistryException(FORBIDDEN, error))
			} else handle.next(it)
		}

	private fun Mono<UserModel>.validateNotLastRoleLevel0(error: String) = flatMap { userToUpdate ->
		val roleLevel = roleService.getLevelByUserRole(userToUpdate.role)
		if (Objects.isNull(roleLevel) || roleLevel!! > 0) {
			return@flatMap Mono.just(userToUpdate)
		}

		port.findByRoleLevel(roleLevel = 0, visibilitySearched = true)
			.filter { userToUpdate.id != it.id }
			.collectList()
			.handle { it, handle ->
				if (it.isEmpty()) {
					log.warn("The user {} is the last administrator of the application", userToUpdate.id)
					handle.error(RegistryException(CONFLICT, error))
				} else handle.next(userToUpdate)
			}
	}

	private fun Mono<UserModel>.validateNotLastProjectRoleLevel0(error: String) = flatMap {
		userProjectProfileService.validateNotLastProjectRoleLevel0(it.id!!, projectId = null, it, error)
	}

	private fun Mono<UserModel>.validateRole(
		currentUser: CurrentUserModel,
		eligibleRoles: List<String>,
		role: String?,
		error: String,
	) = handle { it, handle ->
		if (Objects.nonNull(role) && !eligibleRoles.contains(role)) {
			log.warn("The role {} is not assignable by the user {}", role, currentUser.id)
			handle.error(RegistryException(FORBIDDEN, error))
		} else handle.next(it)
	}
}
