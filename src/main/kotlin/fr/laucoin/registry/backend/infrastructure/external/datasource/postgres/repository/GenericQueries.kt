package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_START_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TABLE

object GenericQueries {
    private const val CREATOR_TABLE = "create_tb"
    const val SELECT_CREATOR = """
        $CREATOR_TABLE.$USER_FIRST_NAME AS $CREATOR_FIRST_NAME,
        $CREATOR_TABLE.$USER_LAST_NAME AS $CREATOR_LAST_NAME,
        $CREATOR_TABLE.$USER_EMAIL AS $CREATOR_EMAIL
    """
    const val CREATOR_JOIN = "LEFT JOIN $USER_TABLE $CREATOR_TABLE ON t.$CREATOR_ID = $CREATOR_TABLE.$ID"

    private const val LAST_EDITOR_TABLE = "editor_tb"
    const val SELECT_LAST_EDITOR = """
        $LAST_EDITOR_TABLE.$USER_FIRST_NAME AS $LAST_MODIFIER_FIRST_NAME,
        $LAST_EDITOR_TABLE.$USER_LAST_NAME AS $LAST_MODIFIER_LAST_NAME,
        $LAST_EDITOR_TABLE.$USER_EMAIL AS $LAST_MODIFIER_EMAIL
    """
    const val LAST_EDITOR_JOIN = "LEFT JOIN $USER_TABLE $LAST_EDITOR_TABLE ON t.$LAST_MODIFIER_ID = $LAST_EDITOR_TABLE.$ID"

    const val LINKED_EVENT_TABLE = "event_tb"
    const val SELECT_LINKED_EVENT = """
        $LINKED_EVENT_TABLE.$ID AS $LINKED_EVENT_ID,
        $LINKED_EVENT_TABLE.$EVENT_NAME AS $LINKED_EVENT_NAME,
        $LINKED_EVENT_TABLE.$EVENT_BEGIN AS $LINKED_EVENT_START_TIME,
        $LINKED_EVENT_TABLE.$EVENT_END AS $LINKED_EVENT_END_TIME,
        $LINKED_EVENT_TABLE.$EVENT_OPTIONS AS $LINKED_EVENT_OPTIONS
    """
    const val EVENT_JOIN =
        "INNER JOIN $EVENT_TABLE $LINKED_EVENT_TABLE ON t.$LINKED_EVENT_ID = $LINKED_EVENT_TABLE.$ID AND $LINKED_EVENT_TABLE.$VISIBLE IS TRUE"

    const val ONLY_VISIBLE_CLAUSE = "(:onlyVisible IS FALSE OR t.$VISIBLE IS TRUE)"

    const val EVENT_CLAUSE = "(t.$LINKED_EVENT_ID = :eventId)"
}
