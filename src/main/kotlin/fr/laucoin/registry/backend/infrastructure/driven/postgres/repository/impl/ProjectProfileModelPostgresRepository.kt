package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IProjectProfileEntityRepository
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
): IProjectProfilePort {
	override fun findProjectProfilesPageByUserId(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel
	): Mono<PageModel<ProjectProfileModel>> {
		return repository.findByUserId(
			userId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.at,
			searchParams.role,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, ProjectProfileEntity::fullCount, mapper::toModel)
	}

	override fun findProjectProfilesPageByProjectId(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>> {
		return repository.findByProjectId(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.at,
			searchParams.role,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, ProjectProfileEntity::fullCount, mapper::toModel)
	}

	override fun findUserIdsWithProjectProfileForProjectWithProfileExclusion(
		projectId: UUID,
		userIds: List<UUID>,
		profileIdToExclude: UUID?,
		status: List<ProfileStatusEnum>,
		from: ZonedDateTime?,
		to: ZonedDateTime?,
	): Flux<UUID> {
		if (userIds.isEmpty()) return Flux.empty()
		return repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			userIds,
			profileIdToExclude,
			status,
			from,
			to
		)
	}

	override fun findProjectProfilesRolesByUserId(userId: UUID): Flux<ProjectProfileRoleModel> {
		return repository.findAllRolesByUserId(
			userId,
			isVisible = null,
			isAvailable = true,
			status = listOf(ACCEPTED),
		).map(roleMapper::toModel)
	}

	override fun findOidcIdsByProjectId(projectId: UUID): Flux<UUID> {
		return repository.findOidcIdsByProjectId(projectId)
	}

	override fun findProjectProfileByUserIdAndId(
		userId: UUID,
		id: UUID,
		isVisible: Boolean?
	): Mono<ProjectProfileModel> {
		return repository.findByUserIdAndId(userId, id, isVisible)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel> {
		return repository.findByProjectIdAndId(projectId, id, isVisible)
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
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
		)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun findProjectProfilesByUserIdAndProjectIds(
		userId: UUID,
		projectIds: List<UUID>,
		searchParams: ProjectProfileSearchParamModel,
	): Flux<ProjectProfileModel> {
		if (projectIds.isEmpty()) return Flux.empty()
		return repository.findByUserIdAndProjectIds(
			userId,
			projectIds,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
		).map(mapper::toModel)
	}

	override fun findLevel0ProjectProfileRoleByUserId(
		userId: UUID,
		isVisible: Boolean?
	): Flux<ProjectProfileRoleCountModel> {
		return repository.findLevel0ProjectProfileRoleByUserId(userId, isVisible).map(roleCountMapper::toModel)
	}

	override fun findLevel0ProjectProfileRoleByProjectId(
		projectId: UUID,
		isVisible: Boolean?
	): Flux<ProjectProfileModel> {
		return repository.findLevel0ProjectProfileRoleByProjectId(projectId, isVisible).map(mapper::toModel)
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

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
