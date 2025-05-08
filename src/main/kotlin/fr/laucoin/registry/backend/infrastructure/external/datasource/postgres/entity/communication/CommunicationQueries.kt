package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE

object CommunicationQueries {
    const val SELECT_COMMUNICATION_SEARCH = """
        CASE
            WHEN :textSearched IS NULL THEN 1
            ELSE similarity(t.message, :textSearched)
        END AS similarity_score
    """

    const val COMMUNICATION_TEXT_SEARCH_CLAUSE = "(:textSearched IS NULL OR similarity(t.message, :textSearched) > 0)"

    private const val LINKED_MOVEMENT_TABLE = "movement_tb"
    private const val LINKED_ACTIVITY_TABLE = "activity_tb"
    const val SELECT_LINKED_MOVEMENT = """
        $LINKED_ACTIVITY_TABLE.$ID AS $COMMUNICATION_ACTIVITY_ID,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_NAME AS $COMMUNICATION_ACTIVITY_NAME,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_DESCRIPTION AS $COMMUNICATION_ACTIVITY_DESCRIPTION,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_DURATION AS $COMMUNICATION_ACTIVITY_DURATION,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_MIN_ALLOWED_PARTICIPANTS AS $COMMUNICATION_ACTIVITY_MIN_ALLOWED_PARTICIPANTS,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_MAX_ALLOWED_PARTICIPANTS AS $COMMUNICATION_ACTIVITY_MAX_ALLOWED_PARTICIPANTS,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_DATE AS $COMMUNICATION_ACTIVITY_START_AVAILABILITY_DATE,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_START_AVAILABILITY_TIME AS $COMMUNICATION_ACTIVITY_START_AVAILABILITY_TIME,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_DATE AS $COMMUNICATION_ACTIVITY_END_AVAILABILITY_DATE,
        $LINKED_ACTIVITY_TABLE.$ACTIVITY_END_AVAILABILITY_TIME AS $COMMUNICATION_ACTIVITY_END_AVAILABILITY_TIME
    """
    const val MOVEMENT_JOIN = """
        INNER JOIN $MOVEMENT_TABLE $LINKED_MOVEMENT_TABLE ON t.$COMMUNICATION_MOVEMENT_ID = $LINKED_MOVEMENT_TABLE.$ID
        INNER JOIN $ACTIVITY_TABLE $LINKED_ACTIVITY_TABLE ON $LINKED_MOVEMENT_TABLE.$COMMUNICATION_ACTIVITY_ID = $LINKED_ACTIVITY_TABLE.$ID
    """

    const val COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE = """
        (
            COALESCE(:startDateTimeSearched, '-infinity'::TIMESTAMP) <= t.$COMMUNICATION_DATE_TIME AND
            COALESCE(:endDateTimeSearched, '+infinity'::TIMESTAMP) >= t.$COMMUNICATION_DATE_TIME
        )
    """
}
