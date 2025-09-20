package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity

object ActivityFields {
	const val ACTIVITY_TABLE = "tb_activity"

	const val ACTIVITY_NAME = "name"
	const val ACTIVITY_DESCRIPTION = "description"
	const val ACTIVITY_DURATION = "duration"
	const val ACTIVITY_MIN_ALLOWED_PARTICIPANTS = "min_allowed_participants"
	const val ACTIVITY_MAX_ALLOWED_PARTICIPANTS = "max_allowed_participants"
	const val ACTIVITY_START_AVAILABILITY_DATE = "start_availability_date"
	const val ACTIVITY_START_AVAILABILITY_TIME = "start_availability_time"
	const val ACTIVITY_END_AVAILABILITY_DATE = "end_availability_date"
	const val ACTIVITY_END_AVAILABILITY_TIME = "end_availability_time"
}
