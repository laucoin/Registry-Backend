package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_FAVORITE_REQUIRES_ACCEPTED_STATUS
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserProjectProfileModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.GenericProfileService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import java.time.OffsetTime
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * [IUserProjectProfileService] implementation: the last-level-0-administrator safeguard query,
 * granting a level-0 Profile when a Project is created or a temporary support Profile is requested,
 * accepting/rejecting an invitation, and the favorite toggle (restricted to ACCEPTED Profiles).
 * Delegates persistence to [IProjectProfilePort], which also computes the per-project counts used by
 * the `includeCounts` decoration directly in SQL. The "Projects requiring attention" dashboard lives
 * on [fr.laucoin.registry.backend.domain.service.IProjectService] instead: it returns Projects, not
 * Profiles.
 */
@Service
class UserProjectProfileService(
	private val port: IProjectProfilePort,
	private val roleService: IRoleService,
	private val transactionalOperator: TransactionalOperator,
): IUserProjectProfileService, GenericProfileService(port) {
	override fun findProjectProfilesPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>>,
		includeCounts: Boolean,
	): Mono<PageModel<UserProjectProfileModel>> {
		return port.findUserProjectProfilesPageByUserId(userId, pageable, searchParams, sortFields, includeCounts)
	}

	override fun <T: GenericModel> validateNotLastProjectRoleLevel0(
		userId: UUID,
		projectId: UUID?,
		result: T,
		error: String
	): Mono<T> {
		return port.findLevel0ProjectProfileRoleByUserId(userId, isVisible = true)
			.filter { Objects.isNull(projectId) || Objects.equals(it.project!!.id, projectId) }
			.collectList()
			.handle { it, handle ->
				val projects = it.filter { profile -> (profile.level0 ?: 0) <= 1 }
				if (projects.isNotEmpty()) {
					log.warn("The user {} is the last administrator of {} project(s)", userId, it.size)
					handle.error(RegistryException(CONFLICT, error, arrayListOf(projects.first().project!!.name)))
				} else handle.next(result)
			}
	}

	override fun createUserProjectProfileFromProject(
		currentUser: CurrentUserModel,
		project: ProjectModel
	): Mono<ProjectProfileModel> {
		val profile = ProjectProfileModel().apply {
			this.projectId = project.id
			this.user = currentUser
			this.role = roleService.getLevel0RoleFromProjectRoles()
			this.status = ACCEPTED
		}
		profile.create(currentUser)

		return port.create(profile)
			.`as`(transactionalOperator::transactional)
	}

	override fun updateUserProjectProfileStatusById(
		currentUser: CurrentUserModel,
		id: UUID,
		status: ProfileStatusEnum
	): Mono<UserProjectProfileModel> {
		return port.findProjectProfileByUserIdAndId(currentUser.id!!, id, isVisible = true)
			.filter { it.status == INVITED }
			.notFoundIfEmpty(id)
			.flatMap { profile ->
				profile.status = status
				profile.update(currentUser)
				port.update(profile)
			}
			.flatMap { withProject(currentUser, it.id!!) }
	}

	override fun toggleFavoriteProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<UserProjectProfileModel> {
		return port.findProjectProfileByUserIdAndId(currentUser.id!!, id, isVisible = true)
			.notFoundIfEmpty(id)
			.flatMap { profile ->
				if (profile.status != ACCEPTED) {
					return@flatMap Mono.error(
						RegistryException(CONFLICT, PROJECT_PROFILE_FAVORITE_REQUIRES_ACCEPTED_STATUS)
					)
				}
				profile.isFavorite = !profile.isFavorite
				profile.update(currentUser)
				port.update(profile)
			}
			.flatMap { withProject(currentUser, it.id!!) }
	}

	override fun createSupportProjectProfile(
		currentUser: CurrentUserModel,
		projectId: UUID
	): Mono<UserProjectProfileModel> {
		val now = CustomDateTimeModel.now()
		val nowPlusOneHour = CustomDateTimeModel.now().plusHours(1)
		val profile = ProjectProfileModel().apply {
			user = currentUser
			role = roleService.getLevel0RoleFromProjectRoles()
			status = ACCEPTED
			startAccess = now
			endAccess = nowPlusOneHour
			create(currentUser)
		}.also { it.projectId = projectId }

		return validateNoProfileConflict(
			projectId,
			listOf(currentUser.id!!),
			profileId = null,
			profile.startAccess!!.toZonedDateTime(OffsetTime.MIN),
			profile.endAccess!!.toZonedDateTime(OffsetTime.MAX),
		)
			.flatMap { port.create(profile) }
			.`as`(transactionalOperator::transactional)
			.flatMap { withProject(currentUser, it.id!!) }
	}

	// The mutation above goes through `port.update`/`port.create`, which only ever return the lean
	// `ProjectProfileModel` (no Project): this re-fetches the same row decorated with its Project,
	// since these endpoints aren't scoped under a Project id and the caller needs to know which one changed.
	private fun withProject(currentUser: CurrentUserModel, id: UUID): Mono<UserProjectProfileModel> {
		return port.findUserProjectProfileByUserIdAndId(currentUser.id!!, id, isVisible = null)
			.notFoundIfEmpty(id)
	}

	override fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return port.findProjectProfileByUserIdAndId(currentUser.id!!, id, isVisible = null)
			.flatMap {
				validateNotLastProjectRoleLevel0(
					it.user!!.id!!,
					it.projectId!!,
					it,
					PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
				)
			}
			.flatMap { port.deleteById(id) }
	}
}
