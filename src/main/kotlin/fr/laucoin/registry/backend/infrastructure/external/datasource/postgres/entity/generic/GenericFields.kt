package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_OPTIONS

object GenericFields {
    const val ID = "id"
    const val VISIBLE = "visible"

    const val CREATED_AT = "created_date"
    const val CREATOR_ID = "created_by"
    private const val CREATOR_PREFIX = "creator_"
    const val CREATOR_FIRST_NAME = "${CREATOR_PREFIX}first_name"
    const val CREATOR_LAST_NAME = "${CREATOR_PREFIX}last_name"
    const val CREATOR_EMAIL = "${CREATOR_PREFIX}email"

    const val LAST_MODIFIER_DATE = "last_modified_date"
    const val LAST_MODIFIER_ID = "last_modified_by"
    private const val LAST_MODIFIER_PREFIX = "last_modifier_"
    const val LAST_MODIFIER_FIRST_NAME = "${LAST_MODIFIER_PREFIX}first_name"
    const val LAST_MODIFIER_LAST_NAME = "${LAST_MODIFIER_PREFIX}last_name"
    const val LAST_MODIFIER_EMAIL = "${LAST_MODIFIER_PREFIX}email"

    private const val EVENT_PREFIX = "event_"
    const val LINKED_EVENT_ID = "$EVENT_PREFIX$ID"
    const val LINKED_EVENT_NAME = "$EVENT_PREFIX$EVENT_NAME"
    const val LINKED_EVENT_START_TIME = "$EVENT_PREFIX$EVENT_BEGIN"
    const val LINKED_EVENT_END_TIME = "$EVENT_PREFIX$EVENT_END"
    const val LINKED_EVENT_OPTIONS = "$EVENT_PREFIX$EVENT_OPTIONS"
}
