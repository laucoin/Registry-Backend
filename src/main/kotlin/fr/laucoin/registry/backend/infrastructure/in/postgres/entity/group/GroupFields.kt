package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantFields.PARTICIPANT_TYPE

object GroupFields {
	const val GROUP_TABLE = "tb_group"
	const val GROUP_CONTENT_TABLE = "tb_group_content"

	const val GROUP_NAME = "name"
	const val GROUP_START_AVAILABILITY_DATE = "start_availability_date"
	const val GROUP_START_AVAILABILITY_TIME = "start_availability_time"
	const val GROUP_END_AVAILABILITY_DATE = "end_availability_date"
	const val GROUP_END_AVAILABILITY_TIME = "end_availability_time"
	const val GROUP_MEMBERS_COUNT = "members_count"
	const val GROUP_INSIDE_MEMBERS_COUNT = "inside_members_count"
	const val GROUP_OUTSIDE_MEMBERS_COUNT = "outside_members_count"

	private const val PARTICIPANT_PREFIX = "participant_"
	const val GROUP_CONTENT_PARTICIPANT_ID = "$PARTICIPANT_PREFIX$ID"
	const val GROUP_CONTENT_PARTICIPANT_FIRST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_FIRST_NAME"
	const val GROUP_CONTENT_PARTICIPANT_LAST_NAME = "$PARTICIPANT_PREFIX$PARTICIPANT_LAST_NAME"
	const val GROUP_CONTENT_PARTICIPANT_BIRTHDAY = "$PARTICIPANT_PREFIX$PARTICIPANT_BIRTHDAY"
	const val GROUP_CONTENT_PARTICIPANT_TYPE = "$PARTICIPANT_PREFIX$PARTICIPANT_TYPE"

	private const val GROUP_PREFIX = "group_"
	const val GROUP_CONTENT_GROUP_ID = "$GROUP_PREFIX$ID"
}
