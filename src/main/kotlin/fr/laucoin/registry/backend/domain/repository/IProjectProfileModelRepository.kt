package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IProjectProfileModelRepository: IGenericReadProjectModelRepository<ProjectProfileModel>,
                                          IGenericWriteModelRepository<ProjectProfileModel> {
    fun findProjectProfilesPageByUserId(
        userId: UUID,
        pageable: PageableModel,
        searchParams: ProjectProfileSearchParamModel,
    ): Mono<PageModel<ProjectProfileModel>>

    fun findProjectProfilesPageByProjectId(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: ProjectProfileSearchParamModel,
    ): Mono<PageModel<ProjectProfileModel>>

    fun findUserIdsWithProjectProfileForProjectWithProfileExclusion(
        projectId: UUID,
        userIds: List<UUID>,
        profileIdToExclude: UUID?,
        statusSearched: List<ProfileStatusEnum> = ProfileStatusEnum.entries.toList(),
        startDateTimeSearched: LocalDateTime? = null,
        endDateTimeSearched: LocalDateTime? = null,
    ): Flux<UUID>

    fun findProjectProfilesRolesByUserId(userId: UUID): Flux<ProjectProfileRoleModel>

    fun findProjectProfileByUserIdAndId(userId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel>

    fun findProjectProfileByProjectAndUserId(
        projectId: UUID,
        userId: UUID,
        searchParams: ProjectProfileSearchParamModel,
    ): Mono<ProjectProfileModel>

    fun findLevel0ProjectProfileRoleByUserId(userId: UUID, visibilitySearched: Boolean?): Flux<ProjectProfileRoleCountModel>

    fun findLevel0ProjectProfileRoleByProjectId(projectId: UUID, visibilitySearched: Boolean?): Flux<ProjectProfileModel>

    fun saveAll(profiles: List<ProjectProfileModel>): Flux<ProjectProfileModel>
}
