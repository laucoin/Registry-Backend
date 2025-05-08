package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IProjectService {
    fun findProjectsPage(
        currentUser: CurrentUserModel,
        pageable: PageableModel,
        withProfile: Boolean,
        searchParams: ProjectSearchParamModel,
    ): Mono<PageModel<ProjectModel>>

    fun findProjectById(id: UUID, visibilitySearched: Boolean?): Mono<ProjectModel>
    fun availableProjectOptions(): Flux<ProjectOptionEnum>
    fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID>
    fun validateDateTimes(id: UUID, start: CustomDateTimeModel?, end: CustomDateTimeModel?, errorCode: String): Mono<UUID>
    fun createProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectModel>
    fun updateProjectById(currentUser: CurrentUserModel, id: UUID, project: ProjectModel): Mono<ProjectModel>
    fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>
    fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectModel>
    fun deleteProjectById(id: UUID): Mono<Void>
}
