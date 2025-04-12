package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_STATUS

object PreferenceFields {
    const val PREFERENCE_TABLE = "tb_preferences"

    const val PREFERENCE_USER_ID = "user_id"

    private const val SELECTED_PROFILE_PREFIX = "selected_profile_"
    private const val EVENT_PREFIX = "event_"
    const val PREFERENCE_SELECTED_PROFILE_ID = "${SELECTED_PROFILE_PREFIX}$ID"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_ID = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$ID"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_NAME = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_NAME"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_START_DATE = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_BEGIN_DATE"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_START_TIME = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_BEGIN_TIME"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_END_DATE = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_END_DATE"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_END_TIME = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_END_TIME"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_OPTIONS = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_OPTIONS"
    const val PREFERENCE_SELECTED_PROFILE_ROLE = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_ROLE"
    const val PREFERENCE_SELECTED_PROFILE_STATUS = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_STATUS"
    const val PREFERENCE_SELECTED_PROFILE_START_ACCESS_DATE = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_START_ACCESS_DATE"
    const val PREFERENCE_SELECTED_PROFILE_START_ACCESS_TIME = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_START_ACCESS_TIME"
    const val PREFERENCE_SELECTED_PROFILE_END_ACCESS_DATE = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_END_ACCESS_DATE"
    const val PREFERENCE_SELECTED_PROFILE_END_ACCESS_TIME = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_END_ACCESS_TIME"
}
