package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_END_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_START_ACCESS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileFields.EVENT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID

object PreferenceFields {
    const val PREFERENCE_TABLE = "tb_preferences"

    const val PREFERENCE_USER_ID = "user_id"

    private const val SELECTED_PROFILE_PREFIX = "selected_profile_"
    private const val EVENT_PREFIX = "event_"
    const val PREFERENCE_SELECTED_PROFILE_ID = "${SELECTED_PROFILE_PREFIX}$ID"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_ID = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$ID"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_NAME = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_NAME"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_START_TIME = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_BEGIN"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_END_TIME = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_END"
    const val PREFERENCE_SELECTED_PROFILE_EVENT_OPTIONS = "${SELECTED_PROFILE_PREFIX}${EVENT_PREFIX}$EVENT_OPTIONS"
    const val PREFERENCE_SELECTED_PROFILE_ROLE = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_ROLE"
    const val PREFERENCE_SELECTED_PROFILE_STATUS = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_STATUS"
    const val PREFERENCE_SELECTED_PROFILE_START_ACCESS = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_START_ACCESS"
    const val PREFERENCE_SELECTED_PROFILE_END_ACCESS = "${SELECTED_PROFILE_PREFIX}$EVENT_PROFILE_END_ACCESS"
}
