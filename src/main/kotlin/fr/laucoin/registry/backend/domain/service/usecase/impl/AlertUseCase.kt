package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.core.IAlertService
import fr.laucoin.registry.backend.domain.service.usecase.IAlertUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des alertes pour la couche REST.
 * Périmètre : Délègue chaque opération au service Alert sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class AlertUseCase(private val service: IAlertService) : IAlertUseCase {
	override fun findAlertsPage(
		currentUser: CurrentUserModel,
		projectId: UUID,
		pageable: PageableModel,
		searchParams: AlertSearchParamModel,
	): Mono<PageModel<AlertModel>> {
		return service.findAlertsPage(currentUser, projectId, pageable, searchParams)
	}

	override fun findAlertStatistics(projectId: UUID): Mono<AlertStatisticsModel> {
		return service.findAlertStatistics(projectId)
	}

	override fun findAlertById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<AlertModel> {
		return service.findAlertById(projectId, id, isVisible)
	}

	override fun findAlertForUser(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel> {
		return service.findAlertForUser(currentUser, projectId, id)
	}

	override fun findAlertCommunicationsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: CommunicationSearchParamModel,
	): Mono<PageModel<CommunicationModel>> {
		return service.findAlertCommunicationsPage(projectId, id, pageable, searchParams)
	}

	override fun createAlert(currentUser: CurrentUserModel, alert: AlertModel): Mono<AlertModel> {
		return service.createAlert(currentUser, alert)
	}

	override fun updateAlertById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		alert: AlertModel,
	): Mono<AlertModel> {
		return service.updateAlertById(currentUser, projectId, id, alert)
	}

	override fun updateAlertStatusById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		status: AlertStatusEnum,
	): Mono<AlertModel> {
		return service.updateAlertStatusById(currentUser, projectId, id, status)
	}

	override fun disableAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel> {
		return service.disableAlertById(currentUser, projectId, id)
	}

	override fun enableAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<AlertModel> {
		return service.enableAlertById(currentUser, projectId, id)
	}

	override fun deleteAlertById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteAlertById(currentUser, projectId, id)
	}
}
