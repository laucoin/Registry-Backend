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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ICommunicationService {
	fun findCommunicationPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
		sort: List<SortModel<CommunicationSortFieldEnum>> = emptyList(),
	): Mono<PageModel<CommunicationModel>>

	fun findCommunicationById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<CommunicationModel>

	fun searchOutMovementWithActivityByText(projectId: UUID, textSearched: String?): Flux<MovementModel>

	fun searchAlertByText(projectId: UUID, textSearched: String?): Flux<AlertModel>

	fun createCommunication(
		currentUser: CurrentUserModel,
		communication: CommunicationModel
	): Mono<CommunicationModel>

	fun updateCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
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
