package fr.laucoin.registry.backend.domain.service.core

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

/**
 * Objectif : Contrat de recherche d'éléments à ajouter à un mouvement et des mouvements planifiés.
 * Périmètre : Déclare la recherche de participants, groupes, véhicules, raisons et activités, ainsi que les mouvements planifiés.
 * Limites : Contrat uniquement ; les limites de résultats viennent de la configuration.
 */
interface IMovementSearchService {
	fun searchParticipantsAndGroupsByText(
		projectId: UUID,
		type: ParticipantTypeEnum,
		query: String?,
		at: ZonedDateTime,
		all: Boolean,
	): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>>

	fun searchVehiclesByText(projectId: UUID, query: String?): Flux<VehicleModel>

	fun searchReasonsByText(
		contentType: ParticipantTypeEnum,
		type: MovementTypeEnum,
	): Flux<MovementReasonEnum>

	fun searchActivitiesByText(
		projectId: UUID,
		contentType: ParticipantTypeEnum,
		query: String?
	): Flux<ActivityModel>

	fun findPlannedMovements(projectId: UUID, type: MovementTypeEnum, date: LocalDate?): Flux<PlannedMovementModel>
}
