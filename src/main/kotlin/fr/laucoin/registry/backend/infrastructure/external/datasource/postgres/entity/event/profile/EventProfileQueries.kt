package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_USER_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TABLE

object EventProfileQueries {
    private const val LINKED_USER_TABLE = "user_tb"
    const val SELECT_LINKED_USER = """
        $LINKED_USER_TABLE.$USER_FIRST_NAME AS $EVENT_PROFILE_USER_FIRST_NAME,
        $LINKED_USER_TABLE.$USER_LAST_NAME AS $EVENT_PROFILE_USER_LAST_NAME,
        $LINKED_USER_TABLE.$USER_EMAIL AS $EVENT_PROFILE_USER_EMAIL,
        $LINKED_USER_TABLE.$USER_LAST_LOGIN AS $EVENT_PROFILE_USER_LAST_LOGIN,
        $LINKED_USER_TABLE.$USER_PURGED AS $EVENT_PROFILE_USER_PURGED
    """
    const val JOIN_USER =
        "INNER JOIN $USER_TABLE $LINKED_USER_TABLE ON t.$EVENT_PROFILE_USER_ID = $LINKED_USER_TABLE.$ID AND $LINKED_USER_TABLE.$VISIBLE IS TRUE"

    const val STATUS_CLAUSE = """
        (:status IS NULL OR t.$EVENT_PROFILE_STATUS = :status)
    """

    const val USABLE_CLAUSE = """
        (:onlyUsable IS FALSE OR (
            (t.$EVENT_PROFILE_START_ACCESS IS NULL OR t.$EVENT_PROFILE_START_ACCESS <= CURRENT_TIMESTAMP)
            AND (t.$EVENT_PROFILE_END_ACCESS IS NULL OR t.$EVENT_PROFILE_END_ACCESS <= CURRENT_TIMESTAMP)
        ))
    """

    const val IN_DATE_RANGE_CLAUSE = """
        (:startAccess IS NULL OR t.$EVENT_PROFILE_END_ACCESS IS NULL OR t.$EVENT_PROFILE_END_ACCESS >= :startAccess) AND
        (:endAccess IS NULL OR t.$EVENT_PROFILE_START_ACCESS IS NULL OR t.$EVENT_PROFILE_START_ACCESS <= :endAccess)
    """
}
