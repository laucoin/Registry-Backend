package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_STATUS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_USER_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_ACCEPTED_PROFILES_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_ACTIVE_ALERTS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_ACTIVITIES_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_GROUPS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_PARTICIPANTS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_VEHICLES_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE

object ProjectQueries {
	const val PROJECT_TEXT_SEARCH_CLAUSE = """
        (:query IS NULL OR UNACCENT(t.$PROJECT_NAME) ILIKE '%' || UNACCENT(:query) || '%')
    """

	const val DATE_IN_PROJECT_DATES_RANGE_CLAUSE = """
        (
            :at IS NULL OR (
                (
                    (COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_BEGIN_TIME, '00:00:00.000000'::TIMETZ)) <= CAST(:at AS TIMESTAMPTZ)
                ) AND
                (
                    (COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_END_TIME, '23:59:59.999999'::TIMETZ)) >= CAST(:at AS TIMESTAMPTZ)
                )
            )
        )
    """

	const val ACCEPTED_PROFILE_STATUS = "ACCEPTED"
	const val ACTIVE_ALERT_STATUS = "IN_PROGRESS"

	private const val COUNT_FROM_PROJECT = "WHERE c.$LINKED_PROJECT_ID = t.id AND c.$VISIBLE IS TRUE"
	const val COUNT_PARTICIPANTS = "(SELECT COUNT(*) FROM $PARTICIPANT_TABLE c $COUNT_FROM_PROJECT)"
	const val COUNT_GROUPS = "(SELECT COUNT(*) FROM $GROUP_TABLE c $COUNT_FROM_PROJECT)"
	const val COUNT_ACCEPTED_PROFILES =
		"(SELECT COUNT(*) FROM $PROJECT_PROFILE_TABLE c $COUNT_FROM_PROJECT AND c.$PROJECT_PROFILE_STATUS = '$ACCEPTED_PROFILE_STATUS')"
	const val COUNT_VEHICLES = "(SELECT COUNT(*) FROM $VEHICLE_TABLE c $COUNT_FROM_PROJECT)"
	const val COUNT_ACTIVITIES = "(SELECT COUNT(*) FROM $ACTIVITY_TABLE c $COUNT_FROM_PROJECT)"
	const val COUNT_ACTIVE_ALERTS =
		"(SELECT COUNT(*) FROM $ALERT_TABLE c $COUNT_FROM_PROJECT AND c.$ALERT_STATUS = '$ACTIVE_ALERT_STATUS')"

	const val SELECT_COUNTS = """
        $COUNT_PARTICIPANTS AS $PROJECT_PARTICIPANTS_COUNT,
        $COUNT_GROUPS AS $PROJECT_GROUPS_COUNT,
        $COUNT_ACCEPTED_PROFILES AS $PROJECT_ACCEPTED_PROFILES_COUNT,
        $COUNT_VEHICLES AS $PROJECT_VEHICLES_COUNT,
        $COUNT_ACTIVITIES AS $PROJECT_ACTIVITIES_COUNT,
        $COUNT_ACTIVE_ALERTS AS $PROJECT_ACTIVE_ALERTS_COUNT
    """

	private const val COUNT_SORT_VALUE = """
        CASE CAST(:sortField AS TEXT)
            WHEN 'participants' THEN $COUNT_PARTICIPANTS
            WHEN 'groups' THEN $COUNT_GROUPS
            WHEN 'acceptedProfiles' THEN $COUNT_ACCEPTED_PROFILES
            WHEN 'vehicles' THEN CASE WHEN 'VEHICLE' = ANY(t.$PROJECT_OPTIONS) THEN $COUNT_VEHICLES END
            WHEN 'activities' THEN CASE WHEN 'ACTIVITY' = ANY(t.$PROJECT_OPTIONS) THEN $COUNT_ACTIVITIES END
            WHEN 'activeAlerts' THEN CASE WHEN 'ALERT' = ANY(t.$PROJECT_OPTIONS) THEN $COUNT_ACTIVE_ALERTS END
        END
    """

	private const val IS_FAVORITE =
		"EXISTS (SELECT 1 FROM $PROJECT_FAVORITE_TABLE f WHERE f.$PROJECT_FAVORITE_PROJECT_ID = t.id AND f.$PROJECT_FAVORITE_USER_ID = CAST(:userId AS UUID))"

	const val PROJECT_FAVORITE_CLAUSE =
		"(CAST(:isFavorite AS BOOLEAN) IS NULL OR $IS_FAVORITE = CAST(:isFavorite AS BOOLEAN))"

	const val PROJECT_SORT = """
        CASE WHEN CAST(:sortField AS TEXT) = 'name' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_NAME END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'name' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN t.$PROJECT_NAME END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'begin' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_BEGIN_TIME, '00:00:00.000000'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'begin' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_BEGIN_DATE, '-infinity'::DATE) + COALESCE(t.$PROJECT_BEGIN_TIME, '00:00:00.000000'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'end' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_END_TIME, '23:59:59.999999'::TIMETZ)) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'end' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN (COALESCE(t.$PROJECT_END_DATE, '+infinity'::DATE) + COALESCE(t.$PROJECT_END_TIME, '23:59:59.999999'::TIMETZ)) END DESC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'favorite' AND NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN CAST($IS_FAVORITE AS INT) END ASC NULLS LAST,
        CASE WHEN CAST(:sortField AS TEXT) = 'favorite' AND COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN CAST($IS_FAVORITE AS INT) END DESC NULLS LAST,
        CASE WHEN NOT COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $COUNT_SORT_VALUE END ASC NULLS LAST,
        CASE WHEN COALESCE(CAST(:isDescending AS BOOLEAN), FALSE) THEN $COUNT_SORT_VALUE END DESC NULLS LAST,
    """
}
