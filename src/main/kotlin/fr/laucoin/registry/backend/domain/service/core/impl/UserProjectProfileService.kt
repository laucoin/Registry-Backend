package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.helper.ProjectProfileFavoriteHelper
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.helper.LastAdministratorHelper
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.GenericProfileService
import fr.laucoin.registry.backend.domain.service.core.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.support.IRoleService
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.OffsetTime
import java.util.UUID

/**
 * Objectif : Gère les profils de l'utilisateur courant à travers les projets.
 * Périmètre : Liste et accepte les invitations, crée les profils de support et protège le dernier administrateur du projet.
 * Limites : Ne gère pas les profils des autres utilisateurs ; voir ProjectProfileService.
 */
@Service
class UserProjectProfileService(
	private val port: IProjectProfilePort,
	private val favorites: ProjectProfileFavoriteHelper,
	private val roleService: IRoleService,
	private val principalCache: IPrincipalCacheService,
	private val lastAdministrator: LastAdministratorHelper,
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
	clock: Clock,
) : IUserProjectProfileService, GenericProfileService(port, clock) {
	override fun findProjectProfilesPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>> {
		return port
			.findProjectProfilesPageByUserId(userId, pageable, searchParams)
			.withFavorites(userId)
	}

	override fun findProjectProfilesById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectProfileModel> {
		return port.findProjectProfileByUserIdAndId(currentUser.id!!, id, isVisible = null)
			.withFavorite(currentUser.id!!)
	}

	private fun Mono<ProjectProfileModel>.withFavorite(userId: UUID): Mono<ProjectProfileModel> =
		flatMap { favorites.withFavorite(userId, it) }

	private fun Mono<PageModel<ProjectProfileModel>>.withFavorites(userId: UUID): Mono<PageModel<ProjectProfileModel>> =
		flatMap { favorites.withFavorites(userId, it) }

	override fun createUserProjectProfileFromProject(
		currentUser: CurrentUserModel,
		project: ProjectModel
	): Mono<ProjectProfileModel> {
		val profile = ProjectProfileModel().apply {
			this.project = project
			this.projectId = project.id
			this.user = currentUser
			this.role = roleService.getLevel0RoleFromProjectRoles()
			this.status = ACCEPTED
		}
		profile.create(currentUser, now())

		return port.create(profile)
			.`as`(transactionalOperator::transactional)
	}

	override fun updateUserProjectProfileStatusById(
		currentUser: CurrentUserModel,
		id: UUID,
		status: ProfileStatusEnum
	): Mono<ProjectProfileModel> {
		return port.findProjectProfileByUserIdAndId(currentUser.id!!, id, isVisible = true)
			.filter { it.status == INVITED }
			.notFoundIfEmpty(id)
			.flatMap { port.update(it.withStatus(currentUser, status)) }
			.doOnNext { principalCache.invalidate(currentUser.oidcId) }
	}

	override fun createSupportProjectProfile(
		currentUser: CurrentUserModel,
		projectId: UUID
	): Mono<ProjectProfileModel> {
		val now = CustomDateTimeModel.now(clock)
		val nowPlusOneHour = CustomDateTimeModel.now(clock).plusHours(1)
		val profile = ProjectProfileModel().apply {
			user = currentUser
			this.projectId = projectId
			role = roleService.getLevel0RoleFromProjectRoles()
			status = ACCEPTED
			startAccess = now
			endAccess = nowPlusOneHour
			create(currentUser, now())
		}

		return rowLockPort.lock(PROJECT, projectId)
			.then(Mono.defer { validateNoSupportConflict(currentUser, projectId, profile) })
			.flatMap { port.create(profile) }
			.`as`(transactionalOperator::transactional)
			.doOnNext { principalCache.invalidate(currentUser.oidcId) }
	}

	private fun validateNoSupportConflict(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profile: ProjectProfileModel,
	): Mono<List<UUID>> {
		return validateNoProfileConflict(
			projectId,
			listOf(currentUser.id!!),
			profileId = null,
			profile.startAccess!!.toZonedDateTime(OffsetTime.MIN),
			profile.endAccess!!.toZonedDateTime(OffsetTime.MAX),
		)
	}

	private fun ProjectProfileModel.withStatus(
		currentUser: CurrentUserModel,
		newStatus: ProfileStatusEnum,
	): ProjectProfileModel = apply {
		status = newStatus
		update(currentUser, now())
	}

	private fun validateNotLastAdministrator(profile: ProjectProfileModel): Mono<ProjectProfileModel> {
		return lastAdministrator.validateNotLastProjectRoleLevel0(
			profile.user!!.id!!,
			profile.projectId!!,
			profile,
			PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
		)
	}

	override fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return port.findProjectProfileByUserIdAndId(currentUser.id!!, id, isVisible = null)
			.notFoundIfEmpty(id)
			.flatMap { validateNotLastAdministrator(it) }
			.flatMap { port.deleteById(id) }
			.`as`(transactionalOperator::transactional)
			.doOnSuccess { principalCache.invalidate(currentUser.oidcId) }
	}
}
