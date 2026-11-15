package fr.laucoin.registry.backend.domain.service.core

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat du service core du profil de projet.
 * Périmètre : Déclare ce que les cas d'usage peuvent demander à propos du profil de projet, y compris la vérification de conflit de plage d'accès.
 * Limites : Contrat uniquement ; les règles métier vivent dans l'implémentation.
 */
interface IProjectProfileService {
	fun findProjectProfilesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>>

	fun findProjectProfileById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ProjectProfileModel>
	fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String>
	fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: List<ProjectProfileModel>,
		users: Map<String, UserModel>,
	): Mono<Pair<List<String>, List<String>>>

	fun updateProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		profile: ProjectProfileModel
	): Mono<ProjectProfileModel>

	fun blockProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ProjectProfileModel>
	fun unblockProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ProjectProfileModel>
	fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
	fun validateNoProfileConflict(
		projectId: UUID,
		userIds: List<UUID>,
		profileId: UUID?,
		startAccess: ZonedDateTime?,
		endAccess: ZonedDateTime?,
	): Mono<List<UUID>>
}
