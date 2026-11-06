package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.helper.LastAdministratorHelper
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.support.IRoleService
import java.time.Clock
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink

/**
 * Objectif : Gère les profils (rôles) des utilisateurs au sein d'un projet.
 * Périmètre : Statut des profils, changements de rôle, conflits de plages d'accès et garde-fous du dernier administrateur.
 * Limites : Ne gère pas les vues de profils de l'utilisateur courant ; voir UserProjectProfileService.
 */
@Service
class ProjectProfileService(
	private val lastAdministrator: LastAdministratorHelper,
	private val port: IProjectProfilePort,
	private val roleService: IRoleService,
	private val principalCache: IPrincipalCacheService,
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
	clock: Clock,
): IProjectProfileService, GenericService(clock) {
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
		isVisible: Boolean?
	): Mono<ProjectProfileModel> {
		return port.findById(projectId, id, isVisible)
			.notFoundIfEmpty(id)
	}

	override fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String> {
		return port.findProjectProfileByProjectAndUserId(
			projectId,
			currentUser.id!!,
			ProjectProfileSearchParamModel(
				isVisible = true,
				isAvailable = true,
				status = listOf(ACCEPTED),
			),
		)
			.notFoundIfEmpty(Pair(projectId, currentUser.id!!))
			.map { roleService.getAssignableProjectRoles(it) }
			.flatMapMany { Flux.fromIterable(it) }
	}

	override fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: List<ProjectProfileModel>,
		users: Map<String, UserModel>,
	): Mono<Pair<List<String>, List<String>>> {
		val emails = profiles.map { it.user!!.email!! }

		return saveAllowedProfiles(currentUser, projectId, profiles, users)
			.map { created -> Pair(created, emails.minus(created.toSet())) }
	}

	private fun saveAllowedProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: List<ProjectProfileModel>,
		users: Map<String, UserModel>,
	): Mono<List<String>> {
		val resolved = profiles.filter { users.containsKey(it.user!!.email) }
			.onEach { it.user = users.getValue(it.user!!.email!!) }
		if (resolved.isEmpty()) return Mono.just(emptyList())

		val emailByUserId = users.entries.associate { it.value.id to it.key }
		return validateNoCreationConflict(projectId, resolved.map { it.user!!.id!! }, resolved.first())
			.map { allowed -> profilesToCreate(currentUser, resolved, allowed) }
			.flatMapMany { port.saveAll(it) }
			.map { emailByUserId.getValue(it.user!!.id) }
			.collectList()
	}

	private fun profilesToCreate(
		currentUser: CurrentUserModel,
		profiles: List<ProjectProfileModel>,
		allowedUserIds: List<UUID>,
	): List<ProjectProfileModel> {
		return profiles.filter { allowedUserIds.contains(it.user!!.id) }
			.onEach { it.create(currentUser, now()) }
	}

	private fun validateNoCreationConflict(
		projectId: UUID,
		userIds: List<UUID>,
		profile: ProjectProfileModel,
	): Mono<List<UUID>> {
		return validateNoProfileConflict(
			projectId,
			userIds,
			profileId = null,
			profile.startAccess?.toZonedDateTime(OffsetTime.MIN),
			profile.endAccess?.toZonedDateTime(OffsetTime.MAX),
		)
	}

	override fun updateProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		profile: ProjectProfileModel
	): Mono<ProjectProfileModel> {
		return rowLockPort.lock(PROJECT, projectId)
			.then(Mono.defer { findProjectProfileById(projectId, id, isVisible = null) })
			.flatMap { validateNoConflictForUpdate(projectId, it, profile) }
			.validateRole(currentUser, projectId, profile)
			.validateNotLastProjectRoleLevel0(PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR)
			.map { it.withAccessOf(profile) }
			.updateProjectProfile(currentUser)
			.`as`(transactionalOperator::transactional)
			.doOnNext { principalCache.invalidate(it.user?.oidcId) }
	}

	override fun blockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ProjectProfileModel> {
		return findProjectProfileById(projectId, id, isVisible = true)
			.validateNotLastProjectRoleLevel0(PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR)
			.updateVisibility(visibility = false)
			.updateProjectProfile(currentUser)
			.`as`(transactionalOperator::transactional)
			.doOnNext { principalCache.invalidate(it.user?.oidcId) }
	}

	override fun unblockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ProjectProfileModel> {
		return findProjectProfileById(projectId, id, isVisible = false)
			.updateVisibility(visibility = true)
			.updateProjectProfile(currentUser)
	}

	override fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return findProjectProfileById(projectId, id, isVisible = null)
			.validateNotLastProjectRoleLevel0(PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR)
			.flatMap { port.deleteById(id).doOnSuccess { _ -> principalCache.invalidate(it.user?.oidcId) } }
			.`as`(transactionalOperator::transactional)
	}

	private fun Mono<ProjectProfileModel>.validateNotLastProjectRoleLevel0(error: String) = flatMap {
		lastAdministrator.validateNotLastProjectRoleLevel0(it.user!!.id!!, it.projectId!!, it, error)
	}

	private fun Mono<ProjectProfileModel>.validateRole(
		currentUser: CurrentUserModel, projectId: UUID, profile: ProjectProfileModel
	): Mono<ProjectProfileModel> = flatMap { validateRoleOf(currentUser, projectId, it, profile) }

	private fun validateRoleOf(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profileToUpdate: ProjectProfileModel,
		profile: ProjectProfileModel,
	): Mono<ProjectProfileModel> {
		val searchParams = ProjectProfileSearchParamModel(isVisible = true, isAvailable = true, status = listOf(ACCEPTED))
		return port.findProjectProfileByProjectAndUserId(projectId, currentUser.id!!, searchParams)
			.handle { assigner, sink -> sink.nextOrError(profileToUpdate, roleErrorFor(currentUser, assigner, profileToUpdate, profile)) }
	}

	private fun roleErrorFor(
		currentUser: CurrentUserModel,
		assigner: ProjectProfileModel,
		profileToUpdate: ProjectProfileModel,
		profile: ProjectProfileModel,
	): RegistryException? {
		val eligibleRoles = roleService.getAssignableProjectRoles(assigner)
		return roleError(currentUser, eligibleRoles, profileToUpdate, profile)
	}

	private fun validateNoConflictForUpdate(
		projectId: UUID,
		existing: ProjectProfileModel,
		profile: ProjectProfileModel,
	): Mono<ProjectProfileModel> {
		return validateNoProfileConflict(
			projectId,
			listOf(existing.user!!.id!!),
			existing.id,
			profile.startAccess?.toZonedDateTime(OffsetTime.MIN),
			profile.endAccess?.toZonedDateTime(OffsetTime.MAX),
		).map { existing }
	}

	private fun ProjectProfileModel.withAccessOf(profile: ProjectProfileModel): ProjectProfileModel = apply {
		role = profile.role
		startAccess = profile.startAccess
		endAccess = profile.endAccess
	}

	private fun roleError(
		currentUser: CurrentUserModel,
		eligibleRoles: List<String>,
		profileToUpdate: ProjectProfileModel,
		profile: ProjectProfileModel,
	): RegistryException? = when {
		!eligibleRoles.contains(profileToUpdate.role) -> {
			log.warn("User \"{}\" cannot update project profile with a role higher up the breast.", currentUser.id)
			RegistryException(
				FORBIDDEN,
				PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER,
				arrayListOf(profileToUpdate.role),
			)
		}

		!eligibleRoles.contains(profile.role) -> {
			log.warn("User \"{}\" tried to update project profile with a role higher up the breast.", currentUser.id)
			RegistryException(FORBIDDEN, PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER, arrayListOf(profile.role))
		}

		else -> null
	}

	private fun Mono<ProjectProfileModel>.updateProjectProfile(currentUser: CurrentUserModel) = flatMap { persistUpdate(currentUser, it) }

	private fun persistUpdate(currentUser: CurrentUserModel, profile: ProjectProfileModel): Mono<ProjectProfileModel> {
		val oidcId = profile.user?.oidcId
		return port.update(profile.apply { update(currentUser, now()) })
			.doOnNext { principalCache.invalidate(oidcId) }
	}

	override fun validateNoProfileConflict(
		projectId: UUID,
		userIds: List<UUID>,
		profileId: UUID?,
		startAccess: ZonedDateTime?,
		endAccess: ZonedDateTime?,
	): Mono<List<UUID>> {
		return port.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			userIds,
			profileId,
			status = listOf(ACCEPTED, INVITED),
			from = startAccess,
			to = endAccess,
		)
			.collectList()
			.handle { conflicting, sink -> sink.nextIfSomeoneIsFree(projectId, userIds, conflicting) }
	}

	private fun SynchronousSink<List<UUID>>.nextIfSomeoneIsFree(projectId: UUID, userIds: List<UUID>, conflicting: List<UUID>) {
		if (conflicting.size != userIds.size) return next(usersWithoutConflict(projectId, userIds, conflicting))
		log.warn("Another profile already exists for the user(s) \"{}\" on the project \"{}\".", userIds, projectId)
		error(RegistryException(CONFLICT, PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE))
	}

	private fun usersWithoutConflict(projectId: UUID, userIds: List<UUID>, conflicting: List<UUID>): List<UUID> {
		if (conflicting.isEmpty()) return userIds

		log.warn(
			"Partial request: a profile already exists for the user(s) \"{}\" on the project \"{}\".",
			userIds.filter { conflicting.contains(it) },
			projectId
		)
		return userIds.filter { !conflicting.contains(it) }
	}
}
