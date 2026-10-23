package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ICommunicationService {
	fun findCommunicationById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<CommunicationModel>

	fun searchOutMovementWithActivityByText(projectId: UUID, query: String?): Flux<MovementModel>

	fun searchAlertByText(projectId: UUID, query: String?): Flux<AlertModel>

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
