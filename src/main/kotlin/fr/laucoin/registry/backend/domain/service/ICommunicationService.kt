package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.CommunicationSortFieldEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Use-case entry point for Communications: search/read (scoped to a Project, a Movement or an
 * Alert), the picker searches used to attach a new one, the create/update/disable/enable/delete
 * lifecycle, and orphan cleanup for the purge job. Callers go through this contract, never the
 * [ICommunicationPort] directly.
 */
interface ICommunicationService {
	fun findCommunicationPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
		sortFields: List<SortModel<CommunicationSortFieldEnum>> = emptyList(),
	): Mono<PageModel<CommunicationModel>>

	fun findCommunicationById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<CommunicationModel>

	fun searchOutMovementWithActivityByText(projectId: UUID, query: String?): Flux<MovementModel>

	fun searchAlertByText(projectId: UUID, query: String?): Flux<AlertModel>

	fun createCommunication(
		currentUser: CurrentUserModel,
		communication: CommunicationModel
	): Mono<CommunicationModel>

	fun disableCommunicationById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<CommunicationModel>
	fun enableCommunicationById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<CommunicationModel>
	fun deleteCommunicationById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
	fun purgeOrphanCommunications(
		movementsToExclude: List<UUID>,
		alertsToExclude: List<UUID>,
		dryRun: Boolean
	): Flux<UUID>
}
