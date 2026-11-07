package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * Objectif : Extrait des colonnes aplaties d'une entité de lecture les entités imbriquées qu'elle porte (mouvement, alerte, activité, utilisateur…).
 * Périmètre : Copie uniquement les colonnes préfixées vers l'entité imbriquée ; les mappers d'entité en tirent ensuite le modèle.
 * Limites : Ne produit aucun modèle et ne porte aucune règle.
 */
@Mapper(config = PostgresMapperConfig::class)
interface NestedColumnsMapper {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "movementId")
	@Mapping(target = "dateTime", source = "movementDateTime")
	@Mapping(target = "type", source = "movementType")
	@Mapping(target = "reason", source = "movementReason")
	@Mapping(target = "activityId", source = "activityId")
	@Mapping(target = "activityName", source = "activityName")
	@Mapping(target = "activityDescription", source = "activityDescription")
	@Mapping(target = "activityDuration", source = "activityDuration")
	@Mapping(target = "activityMinAllowedParticipants", source = "activityMinAllowedParticipants")
	@Mapping(target = "activityMaxAllowedParticipants", source = "activityMaxAllowedParticipants")
	@Mapping(target = "activityStartAvailabilityDate", source = "activityStartAvailabilityDate")
	@Mapping(target = "activityStartAvailabilityTime", source = "activityStartAvailabilityTime")
	@Mapping(target = "activityEndAvailabilityDate", source = "activityEndAvailabilityDate")
	@Mapping(target = "activityEndAvailabilityTime", source = "activityEndAvailabilityTime")
	fun toMovementEntity(entity: CommunicationEntity): MovementEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "alertId")
	@Mapping(target = "dateTime", source = "alertDateTime")
	@Mapping(target = "title", source = "alertTitle")
	@Mapping(target = "status", source = "alertStatus")
	fun toAlertEntity(entity: CommunicationEntity): AlertEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "activityId")
	@Mapping(target = "name", source = "activityName")
	@Mapping(target = "description", source = "activityDescription")
	@Mapping(target = "duration", source = "activityDuration")
	@Mapping(target = "minAllowedParticipants", source = "activityMinAllowedParticipants")
	@Mapping(target = "maxAllowedParticipants", source = "activityMaxAllowedParticipants")
	@Mapping(target = "startAvailabilityDate", source = "activityStartAvailabilityDate")
	@Mapping(target = "startAvailabilityTime", source = "activityStartAvailabilityTime")
	@Mapping(target = "endAvailabilityDate", source = "activityEndAvailabilityDate")
	@Mapping(target = "endAvailabilityTime", source = "activityEndAvailabilityTime")
	fun toActivityEntity(entity: MovementEntity): ActivityEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "preferenceId")
	@Mapping(target = "theme", source = "preferenceTheme", defaultValue = "SYSTEM")
	@Mapping(target = "language", source = "preferenceLanguage")
	@Mapping(target = "refreshMode", source = "preferenceRefreshMode", defaultValue = "NORMAL")
	fun toPreferencesEntity(entity: CurrentUserEntity): PreferencesEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "participantId")
	@Mapping(target = "firstName", source = "participantFirstName")
	@Mapping(target = "lastName", source = "participantLastName")
	@Mapping(target = "birthday", source = "participantBirthday")
	@Mapping(target = "type", source = "participantType")
	fun toParticipantEntity(entity: MovementContentEntity): ParticipantEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "vehicleId")
	@Mapping(target = "licensePlate", source = "vehicleLicensePlate")
	@Mapping(target = "brand", source = "vehicleBrand")
	@Mapping(target = "model", source = "vehicleModel")
	fun toVehicleEntity(entity: MovementContentEntity): VehicleEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "userId")
	@Mapping(target = "firstName", source = "userFirstName")
	@Mapping(target = "lastName", source = "userLastName")
	@Mapping(target = "email", source = "userEmail")
	fun toUserEntity(entity: ParticipantEntity): UserEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "userId")
	@Mapping(target = "oidcId", source = "userOidcId")
	@Mapping(target = "firstName", source = "userFirstName")
	@Mapping(target = "lastName", source = "userLastName")
	@Mapping(target = "email", source = "userEmail")
	@Mapping(target = "lastLogin", source = "userLastLogin")
	fun toUserEntity(entity: ProjectProfileEntity): UserEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "projectId")
	@Mapping(target = "name", source = "projectName")
	fun toProjectEntity(entity: ProjectProfileRoleCountEntity): ProjectEntity
}
