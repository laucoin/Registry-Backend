package fr.laucoin.registry.backend.infrastructure.driving.api.mapper

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.UUID
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * Objectif : Transforme un identifiant reçu dans un DTO en référence minimale vers l'élément du domaine.
 * Périmètre : Crée un modèle ne portant que son identifiant pour les mouvements, alertes, participants, véhicules, utilisateurs, groupes et activités.
 * Limites : Ne charge rien et ne vérifie pas l'existence de l'élément référencé.
 */
@Mapper(injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR)
interface IdReferenceDtoMapper {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	fun toMovement(id: UUID): MovementModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	fun toAlert(id: UUID): AlertModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	fun toParticipant(id: UUID): ParticipantModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	fun toVehicle(id: UUID): VehicleModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	fun toUser(id: UUID): UserModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	fun toGroup(id: UUID): GroupModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "id")
	fun toActivity(id: UUID): ActivityModel
}
