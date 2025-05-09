package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ProjectProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ProjectProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ProjectProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IProjectProfileEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProjectProfileModelPostgresRepository(
    private val repository: IProjectProfileEntityRepository,
    private val mapper: ProjectProfileEntityMapper,
    private val roleMapper: ProjectProfileRoleEntityMapper,
    private val roleCountMapper: ProjectProfileRoleCountEntityMapper,
): IProjectProfileModelRepository {
    override fun findProjectProfilesPageByUserId(
        userId: UUID,
        pageable: PageableModel,
        searchParams: ProjectProfileSearchParamModel
    ): Mono<PageModel<ProjectProfileModel>> {
        return Mono.zip(
            repository.countByUserId(
                userId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
            ),
            repository.findByUserId(
                userId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList(),
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findProjectProfilesPageByProjectId(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: ProjectProfileSearchParamModel,
    ): Mono<PageModel<ProjectProfileModel>> {
        return Mono.zip(
            repository.countByProjectId(
                projectId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
            ),
            repository.findByProjectId(
                projectId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.statusSearched,
                searchParams.dateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList(),
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findUserIdsWithProjectProfileForProjectWithProfileExclusion(
        projectId: UUID,
        userIds: List<UUID>,
        profileIdToExclude: UUID?,
        statusSearched: List<ProfileStatusEnum>,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
    ): Flux<UUID> {
        if (userIds.isEmpty()) return Flux.empty()
        return repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
            projectId,
            userIds,
            profileIdToExclude,
            statusSearched,
            startDateTimeSearched,
            endDateTimeSearched
        )
    }

    override fun findProjectProfilesRolesByUserId(userId: UUID): Flux<ProjectProfileRoleModel> {
        return repository.findAllRolesByUserId(
            userId,
            visibilitySearched = null,
            availabilitySearched = true,
            statusSearched = listOf(ACCEPTED),
        ).map(roleMapper::toModel)
    }

    override fun findProjectProfileByUserIdAndId(userId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel> {
        return repository.findByUserIdAndId(userId, id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel> {
        return repository.findByProjectIdAndId(projectId, id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findProjectProfileByProjectAndUserId(
        projectId: UUID,
        userId: UUID,
        searchParams: ProjectProfileSearchParamModel,
    ): Mono<ProjectProfileModel> {
        return repository.findProjectProfileByProjectAndUserId(
            projectId,
            userId,
            searchParams.visibilitySearched,
            searchParams.availabilitySearched,
            searchParams.statusSearched,
        )
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun findLevel0ProjectProfileRoleByUserId(userId: UUID, visibilitySearched: Boolean?): Flux<ProjectProfileRoleCountModel> {
        return repository.findLevel0ProjectProfileRoleByUserId(userId, visibilitySearched).map(roleCountMapper::toModel)
    }

    override fun findLevel0ProjectProfileRoleByProjectId(projectId: UUID, visibilitySearched: Boolean?): Flux<ProjectProfileModel> {
        return repository.findLevel0ProjectProfileRoleByProjectId(projectId, visibilitySearched).map(mapper::toModel)
    }

    override fun create(element: ProjectProfileModel): Mono<ProjectProfileModel> {
        return save(element)
    }

    override fun update(element: ProjectProfileModel): Mono<ProjectProfileModel> {
        return save(element)
    }

    private fun save(element: ProjectProfileModel): Mono<ProjectProfileModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun saveAll(profiles: List<ProjectProfileModel>): Flux<ProjectProfileModel> {
        return repository.saveAll(profiles.map(mapper::toEntity)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
