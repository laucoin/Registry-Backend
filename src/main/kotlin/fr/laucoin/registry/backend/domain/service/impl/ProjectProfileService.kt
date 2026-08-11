package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_BLOCK_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.PROFILE_BLOCK
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.PROFILE_DELETE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.PROFILE_ROLE_UPDATE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.PROFILE_UNBLOCK
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericProfileService
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.IUserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class ProjectProfileService(
	private val profileService: IUserProjectProfileService,
	private val port: IProjectProfilePort,
	private val roleService: IRoleService,
	private val userPort: IUserPort,
	private val userService: IUserService,
	private val transactionalOperator: TransactionalOperator,
	private val auditService: IAuditService,
	@param:Value($$"${registry.feature.profile.searched.max-user-result}")
	private val maxUserResult: Int,
) : IProjectProfileService, GenericProfileService(port) {
	override fun findProjectProfilesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>> {
		return port
			.findProjectProfilesPageByProjectId(projectId, pageable, searchParams)
	}

	override fun findProjectProfileById(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?
	): Mono<ProjectProfileModel> {
		return port.findById(projectId, id, visibilitySearched)
			.notFoundIfEmpty(id)
	}

	override fun searchUsers(textSearched: String?): Flux<UserModel> {
		return userPort.findWithLimit(
			maxUserResult,
			UserSearchParamModel(textSearched, visibilitySearched = true)
		)
	}

	override fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String> {
		return port.findProjectProfileByProjectAndUserId(
			projectId,
			currentUser.id!!,
			ProjectProfileSearchParamModel(
				visibilitySearched = true,
				availabilitySearched = true,
				statusSearched = listOf(ACCEPTED),
			),
		)
			.notFoundIfEmpty(Pair(projectId, currentUser.id!!))
			.map { roleService.getAssignableProjectRoles(it) }
			.flatMapMany { Flux.fromIterable(it) }
	}

	/**
	 * Creates project profiles for existing users (`userIds`) and/or people invited
	 * by email (`emails`): an unknown email creates an email-only user first, then a
	 * profile is created for every resolved, non-conflicting user from the shared
	 * `template` (role + access window). Wrapped in a transaction so a profile-save
	 * failure never leaves an orphaned invited user behind.
	 *
	 * The template role is validated first, before any email is resolved: the check
	 * mirrors the one [updateProjectProfileById] applies, so a member cannot hand out
	 * a role it could not assign through an update, and a rejected request never
	 * leaves a freshly created email-only user behind.
	 *
	 * The project is reassigned onto every clone: `project` is declared on
	 * [GenericProjectModel], not in [ProjectProfileModel]'s primary constructor, and
	 * Kotlin's generated `copy()` only carries primary-constructor properties — so a
	 * plain `copy()` would silently drop it and every insert would fail the
	 * `project_id` NOT NULL constraint.
	 */
	override fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		userIds: List<UUID>,
		emails: List<String>,
		template: ProjectProfileModel,
	): Mono<Pair<List<UUID>, List<UUID>>> {
		return validateAssignableRole(currentUser, projectId, template.role)
			.flatMap { resolveInvitedUserIds(emails, currentUser) }
			.flatMap { emailUserIds ->
				val allUserIds = (userIds + emailUserIds).distinct()
				validateNoProfileConflict(
					projectId,
					allUserIds,
					profileId = null,
					template.startAccess?.asStart(),
					template.endAccess?.asEnd()
				)
					.map { allowedUsers ->
						allowedUsers.map { userId ->
							template.copy(user = UserModel().apply { id = userId }).apply {
								project = template.project
								create(currentUser)
							}
						}
					}
					.flatMapMany { port.saveAll(it) }
					.collectList()
					.map {
						val savedUserId = it.mapNotNull { profile -> profile.user!!.id }
						Pair(savedUserId, allUserIds.minus(savedUserId.toSet()))
					}
			}
			.`as`(transactionalOperator::transactional)
	}

	/**
	 * Resolves each invite email to a user id, creating an email-only user for any
	 * email with no existing account. `concatMap` keeps resolution sequential so two
	 * identical emails in one request cannot race into a duplicate insert against the
	 * email unique index.
	 */
	private fun resolveInvitedUserIds(emails: List<String>, inviter: CurrentUserModel): Mono<List<UUID>> {
		if (emails.isEmpty()) return Mono.just(emptyList())
		return Flux.fromIterable(emails.distinct())
			.concatMap { userService.findOrCreateInvitedUser(it, inviter) }
			.map { it.id!! }
			.collectList()
	}

	override fun updateProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		profile: ProjectProfileModel
	): Mono<ProjectProfileModel> {
		val updated = findProjectProfileById(projectId, id, visibilitySearched = null)
			.flatMap {
				validateNoProfileConflict(
					projectId,
					listOf(it.user!!.id!!),
					it.id,
					profile.startAccess?.asStart(),
					profile.endAccess?.asEnd(),
				).map { _ -> it }
			}
			.validateRole(currentUser, projectId, profile)
			.validateNotLastProjectRoleLevel0(PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR)
			.map {
				it.apply {
					role = profile.role
					startAccess = profile.startAccess
					endAccess = profile.endAccess
				}
			}
			.updateProjectProfile(currentUser)
		return auditService.audit(updated, currentUser, PROFILE_ROLE_UPDATE, id)
	}

	override fun blockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ProjectProfileModel> {
		val blocked = findProjectProfileById(projectId, id, visibilitySearched = true)
			.validateNotLastProjectRoleLevel0(PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR)
			.validateNotCurrentUser(currentUser, PROJECT_PROFILE_BLOCK_CURRENT_USER)
			.updateVisibility(visibility = false)
			.updateProjectProfile(currentUser)
		return auditService.audit(blocked, currentUser, PROFILE_BLOCK, id)
	}

	override fun unblockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ProjectProfileModel> {
		val unblocked = findProjectProfileById(projectId, id, visibilitySearched = false)
			.updateVisibility(visibility = true)
			.updateProjectProfile(currentUser)
		return auditService.audit(unblocked, currentUser, PROFILE_UNBLOCK, id)
	}

	override fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		val deleted = findProjectProfileById(projectId, id, visibilitySearched = null)
			.validateNotLastProjectRoleLevel0(PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR)
			.validateNotCurrentUser(currentUser, PROJECT_PROFILE_DELETE_CURRENT_USER)
			.flatMap { port.deleteById(id) }
		return auditService.audit(deleted, currentUser, PROFILE_DELETE, id)
	}

	private fun Mono<ProjectProfileModel>.validateNotLastProjectRoleLevel0(error: String) = flatMap {
		profileService.validateNotLastProjectRoleLevel0(it.user!!.id!!, it.project!!.id!!, it, error)
	}

	/**
	 * The member list never acts on the caller's own profile: blocking yourself is
	 * unrecoverable (a blocked profile holds no permission on the project, so it
	 * cannot unblock itself) and removing yourself already has its own door —
	 * leaving from your own memberships, which is a self-service right rather than
	 * an act of administration. The last-administrator guard does not cover either:
	 * it only fires when nobody else administers the project.
	 *
	 * Runs AFTER that guard on purpose. A sole administrator acting on their own
	 * profile trips both, and the last-administrator refusal is the one worth
	 * reporting: it names the project and tells the caller how to proceed (appoint
	 * someone else first), where "not on yourself" would leave them stuck.
	 */
	private fun Mono<ProjectProfileModel>.validateNotCurrentUser(currentUser: CurrentUserModel, error: String) =
		handle { it, handle ->
			if (it.user?.id == currentUser.id) {
				log.warn("The profile \"{}\" belongs to the current user \"{}\".", it.id, currentUser.id)
				handle.error(RegistryException(FORBIDDEN, error))
			} else handle.next(it)
		}

	/**
	 * Roles the caller may hand out on `projectId`, read from its own active profile
	 * there. Empty when the caller has no such profile, which callers propagate as an
	 * empty result rather than a grant: project authorities are derived from exactly
	 * this query, so a caller that reaches a guarded endpoint always has one.
	 */
	private fun findAssignableRoles(currentUser: CurrentUserModel, projectId: UUID): Mono<List<String>> =
		port.findProjectProfileByProjectAndUserId(
			projectId,
			currentUser.id!!,
			ProjectProfileSearchParamModel(
				visibilitySearched = true,
				availabilitySearched = true,
				statusSearched = listOf(ACCEPTED),
			)
		)
			.map { roleService.getAssignableProjectRoles(it) }

	private fun validateAssignableRole(
		currentUser: CurrentUserModel, projectId: UUID, role: String?
	): Mono<List<String>> = findAssignableRoles(currentUser, projectId)
		.handle { eligibleRoles, handle ->
			if (!eligibleRoles.contains(role)) {
				log.warn(
					"User \"{}\" tried to create a project profile with a role higher than its own.",
					currentUser.id,
				)
				handle.error(
					RegistryException(
						FORBIDDEN,
						PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER,
						arrayListOf(role)
					)
				)
			} else handle.next(eligibleRoles)
		}

	private fun Mono<ProjectProfileModel>.validateRole(
		currentUser: CurrentUserModel, projectId: UUID, profile: ProjectProfileModel
	): Mono<ProjectProfileModel> = flatMap { profileToUpdate ->
		findAssignableRoles(currentUser, projectId)
			.handle { eligibleRoles, handle ->
				if (!eligibleRoles.contains(profileToUpdate.role)) {
					log.warn(
						"User \"{}\" cannot update project profile with a role higher up the breast.",
						currentUser.id,
					)
					handle.error(
						RegistryException(
							FORBIDDEN,
							PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER,
							arrayListOf(profileToUpdate.role)
						)
					)
				} else if (!eligibleRoles.contains(profile.role)) {
					log.warn(
						"User \"{}\" tried to update project profile with a role higher up the breast.",
						currentUser.id,
					)
					handle.error(
						RegistryException(
							FORBIDDEN,
							PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER,
							arrayListOf(profile.role)
						)
					)
				} else handle.next(profileToUpdate)
			}
	}

	private fun Mono<ProjectProfileModel>.updateProjectProfile(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser) })
	}
}
