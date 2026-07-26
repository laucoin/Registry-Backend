package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.communication

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_REASON
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TYPE

object CommunicationFields {
	const val COMMUNICATION_TABLE = "tb_communication"

	const val COMMUNICATION_DATE_TIME = "date_time"
	const val COMMUNICATION_MESSAGE = "message"
	const val COMMUNICATION_ON_BEHALF_OF_MOVEMENT = "on_behalf_of_movement"

	private const val COMMUNICATION_MOVEMENT_PREFIX = "movement_"
	const val COMMUNICATION_MOVEMENT_ID = "$COMMUNICATION_MOVEMENT_PREFIX$ID"
	const val COMMUNICATION_MOVEMENT_DATE_TIME = "$COMMUNICATION_MOVEMENT_PREFIX$MOVEMENT_DATE_TIME"
	const val COMMUNICATION_MOVEMENT_TYPE = "$COMMUNICATION_MOVEMENT_PREFIX$MOVEMENT_TYPE"
	const val COMMUNICATION_MOVEMENT_REASON = "$COMMUNICATION_MOVEMENT_PREFIX$MOVEMENT_REASON"

	private const val COMMUNICATION_ACTIVITY_PREFIX = "activity_"
	const val COMMUNICATION_ACTIVITY_ID = "$COMMUNICATION_ACTIVITY_PREFIX$ID"
	const val COMMUNICATION_ACTIVITY_NAME = "$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_NAME"
	const val COMMUNICATION_ACTIVITY_DESCRIPTION = "$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_DESCRIPTION"
	const val COMMUNICATION_ACTIVITY_DURATION = "$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_DURATION"
	const val COMMUNICATION_ACTIVITY_MIN_ALLOWED_PARTICIPANTS =
		"$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_MIN_ALLOWED_PARTICIPANTS"
	const val COMMUNICATION_ACTIVITY_MAX_ALLOWED_PARTICIPANTS =
		"$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_MAX_ALLOWED_PARTICIPANTS"
	const val COMMUNICATION_ACTIVITY_START_AVAILABILITY_DATE =
		"$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_START_AVAILABILITY_DATE"
	const val COMMUNICATION_ACTIVITY_START_AVAILABILITY_TIME =
		"$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_START_AVAILABILITY_TIME"
	const val COMMUNICATION_ACTIVITY_END_AVAILABILITY_DATE =
		"$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_END_AVAILABILITY_DATE"
	const val COMMUNICATION_ACTIVITY_END_AVAILABILITY_TIME =
		"$COMMUNICATION_ACTIVITY_PREFIX$ACTIVITY_END_AVAILABILITY_TIME"

	private const val COMMUNICATION_ALERT_PREFIX = "alert_"
	const val COMMUNICATION_ALERT_ID = "$COMMUNICATION_ALERT_PREFIX$ID"
	const val COMMUNICATION_ALERT_DATE_TIME = "$COMMUNICATION_ALERT_PREFIX$MOVEMENT_DATE_TIME"
	const val COMMUNICATION_ALERT_TITLE = "$COMMUNICATION_ALERT_PREFIX$MOVEMENT_TYPE"
	const val COMMUNICATION_ALERT_STATUS = "$COMMUNICATION_ALERT_PREFIX$MOVEMENT_REASON"
}
