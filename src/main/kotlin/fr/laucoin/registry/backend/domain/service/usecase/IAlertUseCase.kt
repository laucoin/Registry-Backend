package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage des alertes exposé à la couche REST.
 * Périmètre : Reprend les opérations du service Alert appelées par les contrôleurs.
 * Limites : Contrat uniquement ; les règles métier vivent dans le service.
 */
interface IAlertUseCase {
	fun findAlertsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: AlertSearchParamModel,
	): Mono<PageModel<AlertModel>>
	fun findAlertStatistics(projectId: UUID): Mono<AlertStatisticsModel>
	fun findAlertById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<AlertModel>
	fun findAlertForUser(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel>
	fun findAlertCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
	): Mono<PageModel<CommunicationModel>>
	fun createAlert(currentUser: CurrentUserModel, alert: AlertModel): Mono<AlertModel>
	fun updateAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		alert: AlertModel,
	): Mono<AlertModel>
	fun updateAlertStatusById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		status: AlertStatusEnum,
	): Mono<AlertModel>
	fun disableAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel>
	fun enableAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel>
	fun deleteAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
}
