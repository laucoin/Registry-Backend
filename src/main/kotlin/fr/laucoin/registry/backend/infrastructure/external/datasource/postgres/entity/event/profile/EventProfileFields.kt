package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_PURGED

object EventProfileFields {
    const val EVENT_PROFILE_TABLE = "tb_event_profile"

    private const val USER_PREFIX = "user_"
    const val EVENT_PROFILE_USER_ID = "$USER_PREFIX$ID"
    const val EVENT_PROFILE_USER_FIRST_NAME = "$USER_PREFIX$USER_FIRST_NAME"
    const val EVENT_PROFILE_USER_LAST_NAME = "$USER_PREFIX$USER_LAST_NAME"
    const val EVENT_PROFILE_USER_EMAIL = "$USER_PREFIX$USER_EMAIL"
    const val EVENT_PROFILE_USER_LAST_LOGIN = "$USER_PREFIX$USER_LAST_LOGIN"
    const val EVENT_PROFILE_USER_PURGED = "$USER_PREFIX$USER_PURGED"

    const val EVENT_PROFILE_ROLE = "role"
    const val EVENT_PROFILE_STATUS = "status"
    const val EVENT_PROFILE_START_ACCESS = "start_access"
    const val EVENT_PROFILE_END_ACCESS = "end_access"

    const val ROLE_COUNT = "role_count"
}
