package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project

object ProjectFields {
	const val PROJECT_TABLE = "tb_project"

	const val PROJECT_NAME = "name"
	const val PROJECT_BEGIN_DATE = "begin_date"
	const val PROJECT_BEGIN_TIME = "begin_time"
	const val PROJECT_END_DATE = "end_date"
	const val PROJECT_END_TIME = "end_time"
	const val PROJECT_OPTIONS = "options"

	const val PROJECT_PARTICIPANTS_COUNT = "participants_count"
	const val PROJECT_GROUPS_COUNT = "groups_count"
	const val PROJECT_ACCEPTED_PROFILES_COUNT = "accepted_profiles_count"
	const val PROJECT_VEHICLES_COUNT = "vehicles_count"
	const val PROJECT_ACTIVITIES_COUNT = "activities_count"
	const val PROJECT_ACTIVE_ALERTS_COUNT = "active_alerts_count"
}
