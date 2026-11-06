package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum.GROUP
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum.PROFILE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum.PROJECT
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OVERDUE
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Objects
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Construit le libellé de statut des éléments à fenêtre de disponibilité ou de présence, avec la durée écoulée ou restante.
 * Périmètre : Choisit la clé de traduction selon le statut, l'heure courante et les dates de l'élément (activité, groupe, projet, profil, participant, véhicule).
 * Limites : Ne persiste rien ; l'heure vient de l'horloge injectée.
 */
@Component
class StatusLabelMappingHelper(
	private val translateService: ITranslateService,
	private val durationHelper: DurationLabelHelper,
	private val clock: Clock,
) {
	@Named("activityStatus")
	fun activityStatus(model: ActivityModel): LabelDto? =
		model.status?.let { availabilityLabel(it, ACTIVITY, model.startAvailability, model.endAvailability) }

	@Named("groupStatus")
	fun groupStatus(model: GroupModel): LabelDto? =
		model.status?.let { availabilityLabel(it, GROUP, model.startAvailability, model.endAvailability) }

	@Named("projectStatus")
	fun projectStatus(model: ProjectModel): LabelDto? =
		model.status?.let { availabilityLabel(it, PROJECT, model.begin, model.end) }

	@Named("profileAvailabilityStatus")
	fun profileAvailabilityStatus(model: ProjectProfileModel): LabelDto? =
		model.availabilityStatus?.let { availabilityLabel(it, PROFILE, model.startAccess, model.endAccess) }

	@Named("participantStatus")
	fun participantStatus(model: ParticipantModel): LabelDto? =
		model.status?.let { presenceLabel(it, model.lastMovement, model.startAvailability, model.endAvailability) }

	@Named("vehicleStatus")
	fun vehicleStatus(model: VehicleModel): LabelDto? =
		model.status?.let { presenceLabel(it, model.lastMovement, model.startAvailability, model.endAvailability) }

	private fun availabilityLabel(
		model: AvailabilityStatusEnum,
		subject: AvailabilitySubjectEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): LabelDto {
		return LabelDto(model.name, availabilityText(model, subject, startAvailability, endAvailability))
	}

	private fun availabilityText(
		model: AvailabilityStatusEnum,
		subject: AvailabilitySubjectEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now(clock)
		val durationPrefix = "${subject.keyPrefix}$DURATION_PREFIX"
		return when {
			Objects.nonNull(startAvailability) && AvailabilityStatusEnum.AVAILABLE === model ->
				durationHelper.durationMessage("$durationPrefix$model", durationHelper.sinceStart(now, startAvailability))

			startAvailability.asStartIsAfterOther(now) ->
				durationHelper.durationMessage("${durationPrefix}NOT_YET", durationHelper.untilStart(now, startAvailability))

			endAvailability.asEndIsBeforeOther(now) ->
				durationHelper.durationMessage("${durationPrefix}NO_MORE", durationHelper.sinceEnd(now, endAvailability))

			else -> translateService.getMessage(code = "${subject.keyPrefix}$model")
		}
	}

	private fun presenceLabel(
		model: PresenceStatusEnum,
		lastMovement: ZonedDateTime?,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): LabelDto {
		return LabelDto(model.name, presenceText(model, lastMovement, startAvailability, endAvailability))
	}

	private fun presenceText(
		model: PresenceStatusEnum,
		lastMovement: ZonedDateTime?,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now(clock)
		return when {
			model == OVERDUE -> translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model")

			Objects.nonNull(lastMovement) && listOf(IN, OUT).contains(model) ->
				durationHelper.durationMessage(
					"$PRESENCE_STATUS_DURATION_PREFIX$model",
					Duration.between(lastMovement, now.toZonedDateTime()),
				)

			Objects.nonNull(startAvailability) && now.asStartIsAfterOther(startAvailability) ->
				durationHelper.durationMessage("${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE", durationHelper.untilStart(now, startAvailability))

			Objects.nonNull(endAvailability) && now.asEndIsBeforeOther(endAvailability) ->
				durationHelper.durationMessage("${PRESENCE_STATUS_DURATION_PREFIX}LEFT", durationHelper.sinceEnd(now, endAvailability))

			Objects.isNull(lastMovement) -> translateService.getMessage(code = "${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET")

			else -> translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model")
		}
	}
}
