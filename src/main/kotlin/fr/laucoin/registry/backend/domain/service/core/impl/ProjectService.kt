package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.helper.PurgeHelper.purgeEach
import org.springframework.http.HttpStatus.FORBIDDEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_RESTORE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.core.IProjectService
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.support.IRoleService
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink

/**
 * Objectif : Gère les projets, leurs options, compteurs et favoris.
 * Périmètre : Cycle de vie du projet.
 * Limites : Ne gère pas les profils de projet au-delà du profil initial.
 */
@Service
class ProjectService(
	private val port: IProjectPort,
	private val profilePort: IProjectProfilePort,
	private val favoritePort: IProjectFavoritePort,
	private val transactionalOperator: TransactionalOperator,
	private val roleService: IRoleService,
	private val principalCache: IPrincipalCacheService,
	clock: Clock,
): IProjectService, GenericService(clock) {
	override fun findProjectsPage(
		currentUser: CurrentUserModel,
		pageable: PageableModel,
		withProfile: Boolean?,
		searchParams: ProjectSearchParamModel,
	): Mono<PageModel<ProjectModel>> {
		// Sans l'autorité de lecture globale, seuls les projets sur lesquels l'utilisateur a un profil valide sont visibles
		val canReadAll = currentUser.hasAuthority(REGISTRY_PROJECT_R)
		if (!canReadAll && withProfile == false) {
			return Mono.error(RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION))
		}

		searchParams.userId = currentUser.id
		searchParams.hiddenReadableIds = currentUser.projectIdsWith(REGISTRY_PROJECT_RESTORE)
		return (if (!(withProfile ?: true)) {
			port.findPage(pageable, searchParams)
		} else port.findPage(
			roleService.getProjectIdsFromCurrentUserProfiles(currentUser),
			pageable,
			searchParams
		)).withCurrentUserProfiles(currentUser).withFavorites(currentUser)
	}

	override fun findProjectWithProfileById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel> {
		return findProjectById(id, isVisible = null)
			.withCurrentUserProfile(currentUser)
			.withFavorite(currentUser)
	}

	private fun Mono<ProjectModel>.withCurrentUserProfile(currentUser: CurrentUserModel): Mono<ProjectModel> =
		flatMap { attachCurrentUserProfile(currentUser, it) }

	private fun attachCurrentUserProfile(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
		return profilePort.findProjectProfilesByUserIdAndProjectIds(
			currentUser.id!!,
			listOf(project.id!!),
			ProjectProfileSearchParamModel(isVisible = null)
		)
			.collectList()
			.mapNotNull { profiles -> profiles.maxByOrNull { it.priority() } }
			.map { project.apply { profile = it } }
			.defaultIfEmpty(project)
	}

	private fun Mono<PageModel<ProjectModel>>.withCurrentUserProfiles(currentUser: CurrentUserModel): Mono<PageModel<ProjectModel>> =
		flatMap { attachCurrentUserProfiles(currentUser, it) }

	private fun attachCurrentUserProfiles(
		currentUser: CurrentUserModel,
		page: PageModel<ProjectModel>,
	): Mono<PageModel<ProjectModel>> {
		return profilePort.findProjectProfilesByUserIdAndProjectIds(
			currentUser.id!!,
			page.content.mapNotNull { it.id },
			ProjectProfileSearchParamModel(isVisible = null)
		)
			.collectList()
			.map { found -> found.groupBy { it.projectId!! }.mapValues { (_, list) -> list.maxBy { it.priority() } } }
			.map { page.withProfiles(it) }
	}

	private fun PageModel<ProjectModel>.withProfiles(profiles: Map<UUID, ProjectProfileModel>): PageModel<ProjectModel> {
		content.forEach { it.profile = profiles[it.id] }
		return this
	}

	private fun Mono<ProjectModel>.withFavorite(currentUser: CurrentUserModel): Mono<ProjectModel> =
		flatMap { attachFavorite(currentUser, it) }

	private fun attachFavorite(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
		return favoritePort.findFavoriteProjectIds(currentUser.id!!, listOf(project.id!!))
			.hasElements()
			.map { project.apply { favorite = it } }
	}

	private fun Mono<PageModel<ProjectModel>>.withFavorites(currentUser: CurrentUserModel): Mono<PageModel<ProjectModel>> =
		flatMap { attachFavorites(currentUser, it) }

	private fun attachFavorites(currentUser: CurrentUserModel, page: PageModel<ProjectModel>): Mono<PageModel<ProjectModel>> {
		return favoritePort.findFavoriteProjectIds(currentUser.id!!, page.content.mapNotNull { it.id })
			.collectList()
			.map { it.toSet() }
			.map { page.withFavorites(it) }
	}

	private fun PageModel<ProjectModel>.withFavorites(favoriteIds: Set<UUID>): PageModel<ProjectModel> {
		content.forEach { it.favorite = it.id in favoriteIds }
		return this
	}

	override fun addFavoriteProject(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return findProjectById(id, isVisible = null)
			.flatMap { favoritePort.add(currentUser.id!!, id) }
	}

	override fun removeFavoriteProject(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return favoritePort.remove(currentUser.id!!, id)
	}

	override fun findProjectById(id: UUID, isVisible: Boolean?): Mono<ProjectModel> {
		return port.findById(id, isVisible)
			.notFoundIfEmpty(id)
	}

	override fun availableProjectOptions(): Flux<ProjectOptionEnum> {
		return Flux.fromIterable(ProjectOptionEnum.entries)
	}

	override fun createProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
		return port.create(project.apply { create(currentUser, now()) })
	}

	override fun updateProjectById(currentUser: CurrentUserModel, id: UUID, project: ProjectModel): Mono<ProjectModel> {
		return findProjectById(id, isVisible = null)
			.validateDates(project)
			.map { it.applyUpdateOf(project) }
			.updateProject(currentUser)
	}

	private fun ProjectModel.applyUpdateOf(project: ProjectModel): ProjectModel = apply {
		name = project.name
		begin = project.begin
		end = project.end
		options = project.options
	}

	private fun Mono<ProjectModel>.updateProject(currentUser: CurrentUserModel) = flatMap { persistUpdate(currentUser, it) }

	private fun persistUpdate(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
		return port.update(project.apply { update(currentUser, now()) })
			.flatMap { invalidateProjectMembers(it.id!!).thenReturn(it) }
			.withCurrentUserProfile(currentUser)
			.withFavorite(currentUser)
			.`as`(transactionalOperator::transactional)
			.flatMap { invalidateProjectMembers(it.id!!).thenReturn(it) }
	}

	private fun invalidateProjectMembers(projectId: UUID): Mono<List<UUID>> {
		return profilePort.findOidcIdsByProjectId(projectId)
			.collectList()
			.doOnNext { principalCache.invalidateAll(it) }
	}

	private fun Mono<ProjectModel>.validateDates(project: ProjectModel): Mono<ProjectModel> = flatMap {
		if (project.begin.asStartIsAfterOther(it.begin) || project.end.asEndIsBeforeOther(it.end)) {
			validateDatesAgainstElements(it, project)
		} else Mono.just(it)
	}

	private fun validateDatesAgainstElements(existing: ProjectModel, project: ProjectModel): Mono<ProjectModel> {
		return port.validDateTime(
			existing.id!!,
			project.begin?.toZonedDateTime(OffsetTime.MIN),
			project.end?.toZonedDateTime(OffsetTime.MAX)
		)
			.handle { valid, sink -> sink.nextIfValid(valid, existing) }
	}

	private fun SynchronousSink<ProjectModel>.nextIfValid(valid: Boolean, existing: ProjectModel) {
		if (valid) return next(existing)
		log.warn("Failed, {} is out of project range [{}, {}]", existing, existing.begin, existing.end)
		error(RegistryException(CONFLICT, PROJECT_DATE_CONFLICT_WITH_ELEMENTS, arrayListOf(existing.name)))
	}

	override fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel> {
		return findProjectById(id, isVisible = true)
			.updateVisibility(visibility = false)
			.updateProject(currentUser)
	}

	override fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel> {
		return findProjectById(id, isVisible = false)
			.updateVisibility(visibility = true)
			.updateProject(currentUser)
	}

	override fun deleteProjectById(id: UUID): Mono<Unit> {
		return findProjectById(id, isVisible = null)
			.flatMap { deleteAndInvalidate(id) }
	}

	private fun deleteAndInvalidate(id: UUID): Mono<Unit> {
		return profilePort.findOidcIdsByProjectId(id)
			.collectList()
			.flatMap { oidcIds -> port.deleteById(id).doOnSuccess { principalCache.invalidateAll(oidcIds) } }
	}

	override fun purgeProjectsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging projects inactive since {}", dateThreshold)
		return port.findProjectsEligibleForPurge(dateThreshold)
			.purgeEach("project", dryRun, port::deleteById)
	}

	/**
	 * Un utilisateur peut détenir plusieurs profils sur un projet (permanent, support temporaire, bloqué, rejeté...) :
	 * celui qui donne réellement accès, c'est-à-dire visible, accepté et disponible, passe en premier.
	 */
	private fun ProjectProfileModel.priority(): Int =
		(if (visible) 4 else 0) + (if (status == ACCEPTED) 2 else 0) + (if (availabilityStatus == AVAILABLE) 1 else 0)
}
