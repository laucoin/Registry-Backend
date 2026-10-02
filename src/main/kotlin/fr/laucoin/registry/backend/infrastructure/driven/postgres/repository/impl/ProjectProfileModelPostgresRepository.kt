package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserProjectProfileModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.UserProjectProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.ProjectProfileJooqRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * [IProjectProfilePort] implementation: translates every call to [ProjectProfileJooqRepository] and
 * maps [ProjectProfileEntity]/role rows ↔ their domain models via the corresponding mappers. No
 * business logic of its own.
 */
@Service
class ProjectProfileModelPostgresRepository(
	private val repository: ProjectProfileJooqRepository,
	private val mapper: ProjectProfileEntityMapper,
	private val userProjectProfileMapper: UserProjectProfileEntityMapper,
	private val roleMapper: ProjectProfileRoleEntityMapper,
	private val roleCountMapper: ProjectProfileRoleCountEntityMapper,
): IProjectProfilePort {
	override fun findAllByCreatorId(userId: UUID): Flux<ProjectProfileModel> {
		return repository.findAllByCreatorId(userId).map(mapper::toModel)
	}

	override fun findProjectProfilesPageByUserId(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>>,
		includeCounts: Boolean,
	): Mono<PageModel<ProjectProfileModel>> {
		return repository.findByUserId(
			userId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.dateTime,
			searchParams.isFavorite,
			searchParams.isUpcoming,
			sortFields,
			pageable.limit,
			pageable.offset,
			includeCounts,
		).toPageModel(pageable, ProjectProfileEntity::fullCount, mapper::toModel)
	}

	override fun findUserProjectProfilesPageByUserId(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>>,
		includeCounts: Boolean,
	): Mono<PageModel<UserProjectProfileModel>> {
		return repository.findByUserId(
			userId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.dateTime,
			searchParams.isFavorite,
			searchParams.isUpcoming,
			sortFields,
			pageable.limit,
			pageable.offset,
			includeCounts,
		).toPageModel(pageable, ProjectProfileEntity::fullCount, userProjectProfileMapper::toModel)
	}

	override fun findProjectsRequiringAttentionByUserId(userId: UUID, limit: Int): Flux<ProjectModel> {
		return repository.findAcceptedByUserId(userId, limit)
			.map(userProjectProfileMapper::toModel)
			.map(::toProjectWithActiveProfile)
	}

	private fun toProjectWithActiveProfile(profile: UserProjectProfileModel): ProjectModel {
		val activeProfileModel = ProjectProfileModel(
			role = profile.role,
			availabilityStatus = profile.availabilityStatus,
			status = profile.status,
			startAccess = profile.startAccess,
			endAccess = profile.endAccess,
			isFavorite = profile.isFavorite,
		).apply {
			id = profile.id
			isVisible = profile.isVisible
			creation = profile.creation
			lastEdition = profile.lastEdition
		}

		val project = profile.project!!
		project.activeProfile = activeProfileModel
		return project
	}

	override fun findProjectProfilesPageByProjectId(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>>,
	): Mono<PageModel<ProjectProfileModel>> {
		return repository.findByProjectId(
			projectId,
			searchParams.query,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
			searchParams.dateTime,
			sortFields,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, ProjectProfileEntity::fullCount, mapper::toModel)
	}

	override fun findUserIdsWithProjectProfileForProjectWithProfileExclusion(
		projectId: UUID,
		userIds: List<UUID>,
		profileIdToExclude: UUID?,
		status: List<ProfileStatusEnum>,
		startDateTime: ZonedDateTime?,
		endDateTime: ZonedDateTime?,
	): Flux<UUID> {
		if (userIds.isEmpty()) return Flux.empty()
		return repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			userIds,
			profileIdToExclude,
			status,
			startDateTime,
			endDateTime
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
	}

	override fun findUserProjectProfileByUserIdAndId(
		userId: UUID,
		id: UUID,
		isVisible: Boolean?,
	): Mono<UserProjectProfileModel> {
		return repository.findByUserIdAndId(userId, id, isVisible)
			.map(userProjectProfileMapper::toModel)
	}

	override fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel> {
		return repository.findByProjectIdAndId(projectId, id, isVisible)
			.map(mapper::toModel)
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
	}

	override fun findProjectProfilesByProjectIdsAndUserId(
		projectIds: List<UUID>,
		userId: UUID,
		searchParams: ProjectProfileSearchParamModel,
	): Flux<ProjectProfileModel> {
		return repository.findProjectProfilesByProjectIdsAndUserId(
			projectIds,
			userId,
			searchParams.isVisible,
			searchParams.isAvailable,
			searchParams.status,
		)
			.map(mapper::toModel)
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
