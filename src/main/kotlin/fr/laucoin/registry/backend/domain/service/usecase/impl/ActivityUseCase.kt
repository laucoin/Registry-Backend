package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.core.IActivityService
import fr.laucoin.registry.backend.domain.service.usecase.IActivityUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des activités pour la couche REST.
 * Périmètre : Délègue chaque opération au service Activity sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class ActivityUseCase(private val service: IActivityService) : IActivityUseCase {
	override fun findActivitiesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
	): Mono<PageModel<ActivityModel>> {
		return service.findActivitiesPage(projectId, pageable, searchParams)
	}

	override fun findActivityStatistics(projectId: UUID, id: UUID): Mono<ActivityStatisticsModel> {
		return service.findActivityStatistics(projectId, id)
	}

	override fun findActivityById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ActivityModel> {
		return service.findActivityById(projectId, id, isVisible)
	}

	override fun findActivityMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>> {
		return service.findActivityMovementsPage(projectId, id, pageable, searchParams)
	}

	override fun createActivity(currentUser: CurrentUserModel, activity: ActivityModel): Mono<ActivityModel> {
		return service.createActivity(currentUser, activity)
	}

	override fun updateActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		activity: ActivityModel,
	): Mono<ActivityModel> {
		return service.updateActivityById(currentUser, projectId, id, activity)
	}

	override fun disableActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ActivityModel> {
		return service.disableActivityById(currentUser, projectId, id)
	}

	override fun enableActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ActivityModel> {
		return service.enableActivityById(currentUser, projectId, id)
	}

	override fun deleteActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteActivityById(currentUser, projectId, id)
	}
}
