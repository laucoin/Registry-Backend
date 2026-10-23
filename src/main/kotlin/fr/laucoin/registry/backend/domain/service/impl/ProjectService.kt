package fr.laucoin.registry.backend.domain.service.impl

import org.springframework.http.HttpStatus.FORBIDDEN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
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
import fr.laucoin.registry.backend.domain.service.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Manages projects, their options, counters and favorites.
 * Scope: Project lifecycle and the creation of the initial administrator profile.
 * Limits: Does not manage project profiles beyond the initial one.
 */
@Service
class ProjectService(
	private val port: IProjectPort,
	private val profilePort: IProjectProfilePort,
	private val favoritePort: IProjectFavoritePort,
	private val userProjectProfileService: IUserProjectProfileService,
	private val transactionalOperator: TransactionalOperator,
	private val roleService: IRoleService,
	private val principalCache: IPrincipalCacheService,
): IProjectService, GenericService() {
	override fun findProjectsPage(
		currentUser: CurrentUserModel,
		pageable: PageableModel,
		withProfile: Boolean?,
		searchParams: ProjectSearchParamModel,
	): Mono<PageModel<ProjectModel>> {
		// Without the global read authority, only the Projects the User has a valid Profile on are visible
		val canReadAll = currentUser.hasAuthority(REGISTRY_PROJECT_R)
		if (!canReadAll && withProfile == false) {
			return Mono.error(RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION))
		}

		searchParams.userId = currentUser.id
		return (if (!(withProfile ?: !canReadAll)) {
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

	private fun Mono<PageModel<ProjectModel>>.withCurrentUserProfiles(currentUser: CurrentUserModel): Mono<PageModel<ProjectModel>> =
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

	override fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID> {
		return findProjectById(id, isVisible = null)
			.handle { it, handle ->
				if (it.isNotInRange(dateTime)) {
					log.warn("Failed to editing, date {} is out of project range [{}, {}]", dateTime, it.begin, it.end)
					handle.error(
						RegistryException(
							status = UNPROCESSABLE_CONTENT,
							code = errorCode,
							args = arrayListOf(dateTime.toString(), it.begin.toString(), it.end.toString()),
						)
					)
				} else handle.next(id)
			}
	}

	override fun validateDateTimes(
		id: UUID,
		start: CustomDateTimeModel?,
		end: CustomDateTimeModel?,
		errorCode: String
	): Mono<UUID> {
		return findProjectById(id, isVisible = null)
			.handle { it, handle ->
				if (it.isNotInRange(start) || it.isNotInRange(end)) {
					log.warn(
						"Failed to editing, one or more dates ({}, {}) are out of project range [{}, {}]",
						start,
						end,
						it.begin,
						it.end
					)
					handle.error(
						RegistryException(
							status = UNPROCESSABLE_CONTENT,
							code = errorCode,
							args = arrayListOf(
								start.toString(),
								end.toString(),
								it.begin.toString(),
								it.end.toString()
							),
						)
					)
				} else handle.next(id)
			}
	}

	override fun createProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
		return port.create(project.apply { create(currentUser) })
			.flatMap {
				userProjectProfileService.createUserProjectProfileFromProject(currentUser, it)
					.thenReturn(it)
			}
			.withCurrentUserProfile(currentUser)
			.`as`(transactionalOperator::transactional)
			.doOnNext { principalCache.invalidate(currentUser.oidcId) }
	}

	override fun updateProjectById(currentUser: CurrentUserModel, id: UUID, project: ProjectModel): Mono<ProjectModel> {
		return findProjectById(id, isVisible = null)
			.validateDates(project)
			.map {
				it.apply {
					name = project.name
					begin = project.begin
					end = project.end
					options = project.options
				}
			}
			.updateProject(currentUser)
	}

	private fun Mono<ProjectModel>.updateProject(currentUser: CurrentUserModel) = flatMap { project ->
		port.update(project.apply { update(currentUser) })
			.flatMap { updated ->
				profilePort.findOidcIdsByProjectId(project.id!!)
					.collectList()
					.doOnNext { principalCache.invalidateAll(it) }
					.thenReturn(updated)
			}
			.withCurrentUserProfile(currentUser)
			.withFavorite(currentUser)
			.`as`(transactionalOperator::transactional)
	}

	private fun Mono<ProjectModel>.validateDates(project: ProjectModel): Mono<ProjectModel> = flatMap {
		if (project.begin.asStartIsAfterOther(it.begin) || project.end.asEndIsBeforeOther(it.end)) {
			port.validDateTime(
				it.id!!,
				project.begin?.toZonedDateTime(OffsetTime.MIN),
				project.end?.toZonedDateTime(OffsetTime.MAX)
			)
				.handle { valid, handle ->
					if (!valid) {
						log.warn("Failed, {} is out of project range [{}, {}]", it, it.begin, it.end)
						handle.error(
							RegistryException(CONFLICT, PROJECT_DATE_CONFLICT_WITH_ELEMENTS, arrayListOf(it.name))
						)
					} else handle.next(it)
				}
		} else Mono.just(it)
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
			.flatMap {
				profilePort.findOidcIdsByProjectId(id).collectList()
					.flatMap { oidcIds ->
						port.deleteById(id).doOnSuccess { _ -> principalCache.invalidateAll(oidcIds) }
					}
			}
	}

	override fun purgeProjectsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging projects inactive since {}", dateThreshold)
		return port.findProjectsEligibleForPurge(dateThreshold)
			.flatMap({
				if (dryRun) {
					log.info("[Dry run] project {} would be deleted", it)
					Mono.just(it)
				} else {
					log.info("Purging project {}", it)
					port.deleteById(it).thenReturn(it)
						.doOnNext { e -> log.info("Project {} was deleted", e) }
						.doOnError { err -> log.error("Failed to purge project {}", it, err) }
				}
			}, PURGE_DELETE_CONCURRENCY)
	}

	/**
	 * A user may hold several profiles on one project (permanent, temporary support, blocked, rejected...):
	 * the one that actually grants access, i.e. visible, accepted and available, comes first.
	 */
	private fun ProjectProfileModel.priority(): Int =
		(if (visible) 4 else 0) + (if (status == ACCEPTED) 2 else 0) + (if (availabilityStatus == AVAILABLE) 1 else 0)
}
