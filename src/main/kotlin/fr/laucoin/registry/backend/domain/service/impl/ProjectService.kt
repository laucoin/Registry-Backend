package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.PROJECT_DELETE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.PROJECT_DISABLE
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.PROJECT_ENABLE
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.OpenAlertProjectModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID

@Service
class ProjectService(
	private val port: IProjectPort,
	private val userProjectProfileService: IUserProjectProfileService,
	private val transactionalOperator: TransactionalOperator,
	private val roleService: IRoleService,
	private val auditService: IAuditService,
) : IProjectService, GenericService() {
	override fun findProjectsPage(
		currentUser: CurrentUserModel,
		pageable: PageableModel,
		withProfile: Boolean,
		searchParams: ProjectSearchParamModel,
		sort: List<SortModel<ProjectSortFieldEnum>>,
	): Mono<PageModel<ProjectModel>> {
		return if (!withProfile) {
			port.findPage(pageable, searchParams, sort)
		} else port.findPage(
			roleService.getProjectIdsFromCurrentUserProfiles(currentUser),
			pageable,
			searchParams,
			sort
		)
	}

	override fun findProjectById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectModel> {
		return port.findById(id, visibilitySearched)
			.notFoundIfEmpty(id)
	}

	override fun availableProjectOptions(): Flux<ProjectOptionEnum> {
		return Flux.fromIterable(ProjectOptionEnum.entries)
	}

	override fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID> {
		return findProjectById(id, visibilitySearched = null)
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
		return findProjectById(id, visibilitySearched = null)
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
			.`as`(transactionalOperator::transactional)
	}

	override fun updateProjectById(currentUser: CurrentUserModel, id: UUID, project: ProjectModel): Mono<ProjectModel> {
		return findProjectById(id, visibilitySearched = null)
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

	private fun Mono<ProjectModel>.updateProject(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser) })
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
						handle.error(RegistryException(CONFLICT, PROJECT_DATE_CONFLICT_WITH_ELEMENTS))
					} else handle.next(it)
				}
		} else Mono.just(it)
	}

	override fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel> {
		val disabled = findProjectById(id, visibilitySearched = true)
			.updateVisibility(visibility = false)
			.updateProject(currentUser)
		return auditService.audit(disabled, currentUser, PROJECT_DISABLE, id)
	}

	override fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel> {
		val enabled = findProjectById(id, visibilitySearched = false)
			.updateVisibility(visibility = true)
			.updateProject(currentUser)
		return auditService.audit(enabled, currentUser, PROJECT_ENABLE, id)
	}

	override fun deleteProjectById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		val deleted = findProjectById(id, visibilitySearched = null)
			.flatMap { port.deleteById(id) }
		return auditService.audit(deleted, currentUser, PROJECT_DELETE, id)
	}

	override fun findOpenAlertProjects(currentUser: CurrentUserModel, limit: Int): Flux<OpenAlertProjectModel> {
		return port.findOpenAlertProjectsByUserId(currentUser.id!!, limit)
	}

	override fun purgeProjectsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging projects inactive since {}", dateThreshold)
		return port.findProjectsEligibleForPurge(dateThreshold)
			.flatMap {
				if (dryRun) {
					log.info("[Dry run] project {} would be deleted", it)
					Mono.just(it)
				} else {
					log.info("Purging project {}", it)
					port.deleteById(it).thenReturn(it)
						.doOnNext { e -> log.info("Project {} was deleted", e) }
						.doOnError { err -> log.error("Failed to purge project {}", it, err) }
				}
			}
	}
}
