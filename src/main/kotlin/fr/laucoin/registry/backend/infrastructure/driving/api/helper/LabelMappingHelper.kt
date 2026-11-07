package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_CRITICALITY_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_REASON_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PARTICIPANT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_FORM_ASK_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Traduit les valeurs énumérées et les codes de l'API en libellés lisibles pour les DTO de lecture.
 * Périmètre : Résout chaque libellé par la clé de traduction préfixée de son type, dans la langue de la requête.
 * Limites : Ne construit aucun DTO ; il ne renvoie que le texte traduit.
 */
@Component
class LabelMappingHelper(private val translateService: ITranslateService) {
	@Named("alertStatusLabel")
	fun alertStatusLabel(status: AlertStatusEnum): String = message(ALERT_STATUS_PREFIX, status)

	@Named("alertCriticalityLabel")
	fun alertCriticalityLabel(criticality: AlertCriticalityEnum): String = message(ALERT_CRITICALITY_PREFIX, criticality)

	@Named("availabilityStatusLabel")
	fun availabilityStatusLabel(status: AvailabilityStatusEnum): String = message(AVAILABILITY_STATUS_PREFIX, status)

	@Named("presenceStatusLabel")
	fun presenceStatusLabel(status: PresenceStatusEnum): String = message(PRESENCE_STATUS_PREFIX, status)

	@Named("movementTypeLabel")
	fun movementTypeLabel(type: MovementTypeEnum): String = message(MOVEMENT_TYPE_PREFIX, type)

	@Named("movementReasonLabel")
	fun movementReasonLabel(reason: MovementReasonEnum): String = message(MOVEMENT_REASON_PREFIX, reason)

	@Named("participantTypeLabel")
	fun participantTypeLabel(type: ParticipantTypeEnum): String = message(PARTICIPANT_TYPE_PREFIX, type)

	@Named("profileStatusLabel")
	fun profileStatusLabel(status: ProfileStatusEnum): String = message(PROJECT_PROFILE_STATUS_PREFIX, status)

	@Named("projectProfileRoleLabel")
	fun projectProfileRoleLabel(role: String): String = message(PROJECT_PROFILE_ROLE_PREFIX, role)

	@Named("userRoleLabel")
	fun userRoleLabel(role: String): String = message(USER_ROLE_PREFIX, role)

	@Named("projectOptionNameLabel")
	fun projectOptionNameLabel(option: ProjectOptionEnum): String = message(PROJECT_OPTION_NAME_PREFIX, option)

	@Named("projectOptionAskLabel")
	fun projectOptionAskLabel(option: ProjectOptionEnum): String = message(PROJECT_OPTION_FORM_ASK_PREFIX, option)

	@Named("activityReasonLabel")
	fun activityReasonLabel(activity: ActivityModel): String {
		return "${activity.name} (${message(MOVEMENT_REASON_PREFIX, "ACTIVITY")})"
	}

	private fun message(prefix: String, key: Any): String = translateService.getMessage(code = "$prefix$key")
}
