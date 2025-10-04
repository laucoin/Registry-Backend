package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IProjectProfilePort {
	fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel>
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
		startDateTimeSearched: ZonedDateTime? = null,
		endDateTimeSearched: ZonedDateTime? = null,
	): Flux<UUID>

	fun findProjectProfilesRolesByUserId(userId: UUID): Flux<ProjectProfileRoleModel>
	fun findProjectProfileByUserIdAndId(userId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel>
	fun findProjectProfileByProjectAndUserId(
		projectId: UUID,
		userId: UUID,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<ProjectProfileModel>

	fun findLevel0ProjectProfileRoleByUserId(
		userId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileRoleCountModel>

	fun findLevel0ProjectProfileRoleByProjectId(
		projectId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileModel>

	fun saveAll(profiles: List<ProjectProfileModel>): Flux<ProjectProfileModel>
	fun create(element: ProjectProfileModel): Mono<ProjectProfileModel>
	fun update(element: ProjectProfileModel): Mono<ProjectProfileModel>
	fun deleteById(id: UUID): Mono<Unit>
}
