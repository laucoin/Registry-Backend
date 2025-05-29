package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isAfter
import fr.laucoin.registry.backend.domain.extension.DateExt.isBefore
import fr.laucoin.registry.backend.domain.extension.DateExt.notInRange
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IProjectModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import java.time.OffsetTime
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProjectService(
    private val repository: IProjectModelRepository,
    private val userProjectProfileService: IUserProjectProfileService,
    private val transactionalOperator: TransactionalOperator,
    private val roleService: IRoleService,
): IProjectService, GenericService() {
    override fun findProjectsPage(
        currentUser: CurrentUserModel,
        pageable: PageableModel,
        withProfile: Boolean,
        searchParams: ProjectSearchParamModel,
    ): Mono<PageModel<ProjectModel>> {
        return if (! withProfile) {
            repository.findPage(pageable, searchParams)
        } else repository.findPage(roleService.getProjectIdsFromCurrentUserProfiles(currentUser), pageable, searchParams)
    }

    override fun findProjectById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectModel> {
        return repository.findById(id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun availableProjectOptions(): Flux<ProjectOptionEnum> {
        return Flux.fromIterable(ProjectOptionEnum.entries)
    }

    override fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID> {
        return findProjectById(id, visibilitySearched = null)
            .handle { it, handle ->
                if (dateTime.notInRange(it.begin, it.end)) {
                    log.warn("Failed to editing, date {} is out of project range [{}, {}]", dateTime, it.begin, it.end)
                    handle.error(
                        RegistryException(
                            status = UNPROCESSABLE_ENTITY,
                            code = errorCode,
                            args = arrayListOf(dateTime.toString(), it.begin.toString(), it.end.toString()),
                        )
                    )
                } else handle.next(id)
            }
    }

    override fun validateDateTimes(id: UUID, start: CustomDateTimeModel?, end: CustomDateTimeModel?, errorCode: String): Mono<UUID> {
        return findProjectById(id, visibilitySearched = null)
            .handle { it, handle ->
                if (start.notInRange(it.begin, it.end) || end.notInRange(it.begin, it.end)) {
                    log.warn(
                        "Failed to editing, one or more dates ({}, {}) are out of project range [{}, {}]",
                        start,
                        end,
                        it.begin,
                        it.end
                    )
                    handle.error(
                        RegistryException(
                            status = UNPROCESSABLE_ENTITY,
                            code = errorCode,
                            args = arrayListOf(start.toString(), end.toString(), it.begin.toString(), it.end.toString()),
                        )
                    )
                } else handle.next(id)
            }
    }

    override fun createProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel> {
        return repository.create(project.apply { create(currentUser) })
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
        repository.update(it.apply { update(currentUser) })
    }

    private fun Mono<ProjectModel>.validateDates(project: ProjectModel): Mono<ProjectModel> = flatMap {
        if (it.begin.isBefore(project.begin) || it.end.isAfter(project.end)) {
            repository.validDateTime(
                it.id !!,
                project.begin?.toZonedDateTime(OffsetTime.MIN),
                project.end?.toZonedDateTime(OffsetTime.MAX)
            )
                .handle { valid, handle ->
                    if (! valid) {
                        log.warn("Failed, {} is out of project range [{}, {}]", it, it.begin, it.end)
                        handle.error(RegistryException(CONFLICT, PROJECT_DATE_CONFLICT_WITH_ELEMENTS))
                    } else handle.next(it)
                }
        } else Mono.just(it)
    }

    override fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel> {
        return findProjectById(id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateProject(currentUser)
    }

    override fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel> {
        return findProjectById(id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateProject(currentUser)
    }

    override fun deleteProjectById(id: UUID): Mono<Void> {
        return findProjectById(id, visibilitySearched = null)
            .flatMap { repository.deleteById(id) }
    }
}
