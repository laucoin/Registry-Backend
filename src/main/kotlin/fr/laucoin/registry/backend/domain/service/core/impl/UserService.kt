package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
import fr.laucoin.registry.backend.domain.helper.PurgeHelper.purgeEach
import org.springframework.dao.DataIntegrityViolationException
import fr.laucoin.registry.backend.domain.service.support.IFeatureService
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CONCURRENT_UPDATE_CONFLICT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.extension.StringExt.toNormalizedEmail
import fr.laucoin.registry.backend.domain.helper.LastAdministratorHelper
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.support.IRoleService
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.LocalDate
import java.util.Objects
import java.util.UUID

/**
 * Objectif : Gère les utilisateurs de la plateforme et leurs rôles.
 * Périmètre : Cycle de vie des utilisateurs, blocage et garde-fous du dernier administrateur.
 * Limites : N'authentifie pas les utilisateurs ; l'authentification est déléguée à l'IDP externe.
 */
@Service
class UserService(
	private val port: IUserPort,
	private val lastAdministrator: LastAdministratorHelper,
	private val transactionalOperator: TransactionalOperator,
	private val roleService: IRoleService,
	private val featureService: IFeatureService,
	private val principalCache: IPrincipalCacheService,
	clock: Clock,
) : ApplicationListener<ContextRefreshedEvent>, IUserService, GenericService(clock) {
	private lateinit var serviceAccount: CurrentUserModel

	// Le blocage est voulu : cela s'exécute une seule fois sur le thread de rafraîchissement du contexte
	override fun onApplicationEvent(event: ContextRefreshedEvent) {
		serviceAccount = port.findServiceAccount().blockOptional()
			.orElseThrow { IllegalStateException("Service account not found") }
	}

	override fun findUsersPage(
		pageable: PageableModel,
		searchParams: UserSearchParamModel
	): Mono<PageModel<UserModel>> {
		return port.findPage(pageable, searchParams)
	}

	override fun findUserById(id: UUID, isVisible: Boolean?): Mono<UserModel> {
		return port.findById(id, isVisible)
			.filter { isNotServiceAccount(it) }
			.notFoundIfEmpty(id)
	}

	private fun isNotServiceAccount(user: UserModel): Boolean = user.id != serviceAccount.id

	override fun findUserByOidcId(id: UUID, isVisible: Boolean?): Mono<CurrentUserModel> {
		return port.findByOidcId(id, isVisible)
			.filter { isNotServiceAccount(it) }
	}

	override fun findUserByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserModel> {
		return port.findByEmail(email, isVisible)
			.filter { isNotServiceAccount(it) }
	}

	private fun findUsersByEmails(emails: Collection<String>): Flux<UserModel> {
		return port.findByEmails(emails.map { it.toNormalizedEmail() }, isVisible = null)
			.filter { isNotServiceAccount(it) }
	}

	private fun createPendingUser(email: String): Mono<UserModel> {
		log.info("Saving pending user (not yet linked to an OIDC ID) in database")
		val user = UserModel(
			email = email.toNormalizedEmail(),
			role = roleService.getDefaultUserRole(),
		)
		user.create(serviceAccount, now())

		return port.create(user)
	}

	override fun resolveUsersByEmail(emails: List<String>): Mono<Map<String, UserModel>> {
		return findUsersByEmails(emails)
			.collectList()
			.flatMap { found -> withPendingUsers(emails, found.associateBy { it.email!!.toNormalizedEmail() }) }
	}

	private fun withPendingUsers(emails: List<String>, known: Map<String, UserModel>): Mono<Map<String, UserModel>> {
		val unknown = emails.filterNot { known.containsKey(it) }
		if (unknown.isEmpty() || !featureService.isInviteByEmailEnabled()) return Mono.just(known)

		return Flux.fromIterable(unknown)
			.concatMap(::createPendingUser)
			.onErrorMap(DataIntegrityViolationException::class.java) {
				RegistryException(CONFLICT, CONCURRENT_UPDATE_CONFLICT)
			}
			.collectList()
			.map { created -> known + created.associateBy { it.email!!.toNormalizedEmail() } }
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
			lastLogin = now()
		)
		user.create(serviceAccount, now())

		return port.create(user)
			.map { CurrentUserModel(it) }
	}

	override fun updateUserIfPersonalDataChanged(
		user: CurrentUserModel,
		email: String,
		firstName: String?,
		lastName: String?
	): Mono<CurrentUserModel> {
		if (!user.personalDataChanged(email, firstName, lastName)) {
			return Mono.just(user)
		}

		log.info("Updating personal data for user \"{}\"", user.id)
		user.email = email
		user.firstName = firstName
		user.lastName = lastName

		return updateUser(serviceAccount, user)
			.map { user }
	}

	override fun recordLoginByOidcId(oidcId: UUID): Mono<Void> {
		return port.findByOidcId(oidcId, isVisible = null)
			.filter { isNotServiceAccount(it) }
			.flatMap { updateUser(serviceAccount, it.apply { lastLogin = now() }) }
			.then()
	}

	override fun linkUserToOidcId(user: CurrentUserModel, oidcId: UUID): Mono<CurrentUserModel> {
		log.info("Linking existing user \"{}\" to OIDC ID \"{}\"", user.id, oidcId)
		user.oidcId = oidcId
		user.lastLogin = now()

		return updateUser(serviceAccount, user).map { user }
	}

	private fun updateUser(currentUser: CurrentUserModel, user: UserModel): Mono<UserModel> {
		return port.update(user.apply { update(currentUser, now()) })
			.doOnNext { principalCache.invalidate(it.oidcId) }
	}

	override fun updateUserRoleById(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserModel> {
		val assignableRoles = roleService.getAssignableUserRoles(currentUser)
		return findUserByIdWithEligibleRole(assignableRoles, id, isVisible = true)
			.validateRole(currentUser, assignableRoles, role, USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN)
			.validateNotLastRoleLevel0(USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE)
			.flatMap { updateUser(currentUser, it.apply { this.role = role }) }
			.`as`(transactionalOperator::transactional)
	}

	private fun findUserByIdWithEligibleRole(
		assignableRoles: List<String>,
		id: UUID,
		isVisible: Boolean?
	): Mono<UserModel> {
		return findUserById(id, isVisible)
			.filter { Objects.isNull(it.role) || assignableRoles.contains(it.role) }
			.notFoundIfEmpty(id)
	}

	override fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
		val allowedRoles = roleService.getAssignableUserRoles(currentUser)
		return findUserByIdWithEligibleRole(allowedRoles, id, isVisible = true)
			.validateNotCurrentUser(currentUser, USER_BLOCK_CURRENT_USER)
			.validateNotLastRoleLevel0(USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR)
			.validateNotLastProjectRoleLevel0(USER_BLOCK_LAST_PROJECT_ADMINISTRATOR)
			.updateVisibility(visibility = false)
			.flatMap { updateUser(currentUser, it) }
			.`as`(transactionalOperator::transactional)
	}

	override fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserModel> {
		val allowedRoles = roleService.getAssignableUserRoles(currentUser)
		return findUserByIdWithEligibleRole(allowedRoles, id, isVisible = false)
			.updateVisibility(visibility = true)
			.flatMap { updateUser(currentUser, it) }
	}

	override fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		val allowedRoles = roleService.getAssignableUserRoles(currentUser)
		return findUserByIdWithEligibleRole(allowedRoles, id, isVisible = null)
			.validateNotCurrentUser(currentUser, USER_DELETE_CURRENT_USER)
			.deleteWithAdministratorGuards()
	}

	override fun deleteCurrentUser(currentUser: CurrentUserModel): Mono<Unit> {
		return findUserById(currentUser.id!!, isVisible = null).deleteWithAdministratorGuards()
	}

	private fun Mono<UserModel>.deleteWithAdministratorGuards(): Mono<Unit> =
		validateNotLastRoleLevel0(USER_DELETE_LAST_APPLICATION_ADMINISTRATOR)
			.validateNotLastProjectRoleLevel0(USER_DELETE_LAST_PROJECT_ADMINISTRATOR)
			.flatMap { port.deleteById(it.id!!).doOnSuccess { _ -> principalCache.invalidate(it.oidcId) } }
			.`as`(transactionalOperator::transactional)

	override fun purgeUsersIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging users inactive since {}", dateThreshold)
		// Séquentiel : les vérifications du "dernier administrateur" dépendent des utilisateurs supprimés plus tôt dans la même exécution
		return port.findUserIdsOlderThanLastLogin(dateThreshold)
			.concatMap(::keepIfPurgeable)
			.purgeEach("user", dryRun, port::deleteById, concurrency = 1)
	}

	private fun keepIfPurgeable(id: UUID): Mono<UUID> {
		return findUserById(id, isVisible = null)
			.validateNotLastRoleLevel0(USER_DELETE_LAST_APPLICATION_ADMINISTRATOR)
			.validateNotLastProjectRoleLevel0(USER_DELETE_LAST_PROJECT_ADMINISTRATOR)
			.map { it.id!! }
			.onErrorResume(RegistryException::class.java) {
				log.info("User {} is not purged: {}", id, it.message)
				Mono.empty()
			}
	}

	private fun Mono<UserModel>.validateNotCurrentUser(currentUser: CurrentUserModel, error: String) =
		handle<UserModel> { user, sink -> sink.nextOrError(user, currentUserError(user, currentUser, error)) }

	private fun currentUserError(user: UserModel, currentUser: CurrentUserModel, error: String): RegistryException? {
		if (user.id != currentUser.id) return null
		log.warn("The user {} is the current user", currentUser.id)
		return RegistryException(FORBIDDEN, error)
	}

	private fun Mono<UserModel>.validateNotLastRoleLevel0(error: String) = flatMap { validateNotLastAdministrator(it, error) }

	private fun validateNotLastAdministrator(userToUpdate: UserModel, error: String): Mono<UserModel> {
		val roleLevel = roleService.getLevelByUserRole(userToUpdate.role)
		if (Objects.isNull(roleLevel) || roleLevel!! > 0) return Mono.just(userToUpdate)

		return port.lockLevel0Users()
			.then(Mono.defer { port.findByRoleLevel(roleLevel = 0, isVisible = true).collectList() })
			.map { administrators -> administrators.any { userToUpdate.id != it.id } }
			.flatMap { rejectIfLastAdministrator(it, userToUpdate, error) }
	}

	private fun rejectIfLastAdministrator(
		hasOtherAdministrator: Boolean,
		userToUpdate: UserModel,
		error: String,
	): Mono<UserModel> {
		if (hasOtherAdministrator) return Mono.just(userToUpdate)
		log.warn("The user {} is the last administrator of the application", userToUpdate.id)
		return Mono.error(RegistryException(CONFLICT, error))
	}

	private fun Mono<UserModel>.validateNotLastProjectRoleLevel0(error: String) = flatMap {
		lastAdministrator.validateNotLastProjectRoleLevel0(it.id!!, projectId = null, it, error)
	}

	private fun Mono<UserModel>.validateRole(
		currentUser: CurrentUserModel,
		eligibleRoles: List<String>,
		role: String?,
		error: String,
	) = handle<UserModel> { user, sink -> sink.nextOrError(user, roleError(currentUser, eligibleRoles, role, error)) }

	private fun roleError(
		currentUser: CurrentUserModel,
		eligibleRoles: List<String>,
		role: String?,
		error: String,
	): RegistryException? {
		if (role in eligibleRoles) return null
		log.warn("The role {} is not assignable by the user {}", role, currentUser.id)
		return RegistryException(FORBIDDEN, error, arrayListOf(role))
	}
}
