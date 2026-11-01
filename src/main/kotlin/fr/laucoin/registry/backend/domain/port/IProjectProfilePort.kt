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

/**
 * Objectif : Port driven donnant au domaine l'accès à la persistance du profil de projet.
 * Périmètre : Déclare les lectures, recherches et écritures du profil de projet dont dépendent les services.
 * Limites : Contrat uniquement ; l'adaptateur Postgres porte le SQL, les entités et le mapping.
 */
interface IProjectProfilePort {
	fun findById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel>
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
		status: List<ProfileStatusEnum> = ProfileStatusEnum.entries.toList(),
		from: ZonedDateTime? = null,
		to: ZonedDateTime? = null,
	): Flux<UUID>

	fun findProjectProfilesRolesByUserId(userId: UUID): Flux<ProjectProfileRoleModel>
	fun findOidcIdsByProjectId(projectId: UUID): Flux<UUID>
	fun findProjectProfileByUserIdAndId(userId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel>
	fun findProjectProfileByProjectAndUserId(
		projectId: UUID,
		userId: UUID,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<ProjectProfileModel>

	fun findProjectProfilesByUserIdAndProjectIds(
		userId: UUID,
		projectIds: List<UUID>,
		searchParams: ProjectProfileSearchParamModel,
	): Flux<ProjectProfileModel>

	fun findLevel0ProjectProfileRoleByUserId(
		userId: UUID,
		isVisible: Boolean?
	): Flux<ProjectProfileRoleCountModel>

	fun lockLevel0ProjectProfilesByUserId(userId: UUID): Flux<UUID>

	fun findLevel0ProjectProfileRoleByProjectId(
		projectId: UUID,
		isVisible: Boolean?
	): Flux<ProjectProfileModel>

	fun saveAll(profiles: List<ProjectProfileModel>): Flux<ProjectProfileModel>
	fun create(element: ProjectProfileModel): Mono<ProjectProfileModel>
	fun update(element: ProjectProfileModel): Mono<ProjectProfileModel>
	fun deleteById(id: UUID): Mono<Unit>
}
