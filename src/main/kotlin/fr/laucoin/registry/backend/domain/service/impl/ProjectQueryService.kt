package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_RESTORE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
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
import fr.laucoin.registry.backend.domain.service.IProjectQueryService
import fr.laucoin.registry.backend.domain.service.IRoleService
import java.time.Clock
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Consulte les projets : page, détail, profil de l'appelant, favoris et options disponibles.
* Périmètre : Lecture seule, avec la visibilité dépendant de l'autorité de lecture globale et du profil de l'appelant.
 * Limites : Ne modifie rien.
 */
@Service
class ProjectQueryService(
	private val port: IProjectPort,
	private val profilePort: IProjectProfilePort,
	private val favoritePort: IProjectFavoritePort,
	private val roleService: IRoleService,
	clock: Clock,
) : IProjectQueryService, GenericService(clock) {
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

	override fun withCurrentUserProfile(project: ProjectModel, currentUser: CurrentUserModel): Mono<ProjectModel> =
		Mono.just(project).withCurrentUserProfile(currentUser)

	override fun withFavorite(project: ProjectModel, currentUser: CurrentUserModel): Mono<ProjectModel> =
		Mono.just(project).withFavorite(currentUser)

	private fun Mono<ProjectModel>.withCurrentUserProfile(currentUser: CurrentUserModel): Mono<ProjectModel> =
		flatMap<ProjectModel> { project ->
			profilePort.findProjectProfilesByUserIdAndProjectIds(
				currentUser.id!!,
				listOf(project.id!!),
				ProjectProfileSearchParamModel(isVisible = null)
			)
				.collectList()
				.mapNotNull { profiles -> profiles.maxByOrNull { it.priority() } }
				.map { project.apply { profile = it } }
				.defaultIfEmpty(project)
		}

	private fun Mono<PageModel<ProjectModel>>.withCurrentUserProfiles(
		currentUser: CurrentUserModel,
	): Mono<PageModel<ProjectModel>> =
		flatMap<PageModel<ProjectModel>> { page ->
			profilePort.findProjectProfilesByUserIdAndProjectIds(
				currentUser.id!!,
				page.content.mapNotNull { it.id },
				ProjectProfileSearchParamModel(isVisible = null)
			)
				.collectList()
				.map { found -> found.groupBy { it.projectId!! }.mapValues { (_, list) -> list.maxBy { it.priority() } } }
				.map { profiles ->
					page.content.forEach { it.profile = profiles[it.id] }
					page
				}
		}

	private fun Mono<ProjectModel>.withFavorite(currentUser: CurrentUserModel): Mono<ProjectModel> =
		flatMap { project ->
			favoritePort.findFavoriteProjectIds(currentUser.id!!, listOf(project.id!!))
				.hasElements()
				.map { project.apply { favorite = it } }
		}

	private fun Mono<PageModel<ProjectModel>>.withFavorites(currentUser: CurrentUserModel): Mono<PageModel<ProjectModel>> =
		flatMap { page ->
			favoritePort.findFavoriteProjectIds(currentUser.id!!, page.content.mapNotNull { it.id })
				.collectList()
				.map { it.toSet() }
				.map { favoriteIds ->
					page.content.forEach { it.favorite = it.id in favoriteIds }
					page
				}
		}

	override fun findProjectById(id: UUID, isVisible: Boolean?): Mono<ProjectModel> {
		return port.findById(id, isVisible)
			.notFoundIfEmpty(id)
	}

	override fun availableProjectOptions(): Flux<ProjectOptionEnum> =
		Flux.fromIterable(ProjectOptionEnum.entries)

	private fun ProjectProfileModel.priority(): Int =
		(if (visible) VISIBLE_WEIGHT else 0) +
			(if (status == ACCEPTED) ACCEPTED_WEIGHT else 0) +
			(if (availabilityStatus == AVAILABLE) AVAILABLE_WEIGHT else 0)

	private companion object {
		const val VISIBLE_WEIGHT = 4
		const val ACCEPTED_WEIGHT = 2
		const val AVAILABLE_WEIGHT = 1
	}
}
