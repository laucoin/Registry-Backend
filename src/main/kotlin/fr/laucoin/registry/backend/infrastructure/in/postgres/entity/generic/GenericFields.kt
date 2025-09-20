package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_OPTIONS

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

	private const val PROJECT_PREFIX = "project_"
	const val LINKED_PROJECT_ID = "$PROJECT_PREFIX$ID"
	const val LINKED_PROJECT_NAME = "$PROJECT_PREFIX$PROJECT_NAME"
	const val LINKED_PROJECT_START_DATE = "$PROJECT_PREFIX$PROJECT_BEGIN_DATE"
	const val LINKED_PROJECT_START_TIME = "$PROJECT_PREFIX$PROJECT_BEGIN_TIME"
	const val LINKED_PROJECT_END_DATE = "$PROJECT_PREFIX$PROJECT_END_DATE"
	const val LINKED_PROJECT_END_TIME = "$PROJECT_PREFIX$PROJECT_END_TIME"
	const val LINKED_PROJECT_OPTIONS = "$PROJECT_PREFIX$PROJECT_OPTIONS"
	const val LINKED_PROJECT_VISIBLE = "$PROJECT_PREFIX$VISIBLE"
}
