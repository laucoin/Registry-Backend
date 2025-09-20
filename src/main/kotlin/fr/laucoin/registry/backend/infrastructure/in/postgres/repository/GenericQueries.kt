package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.CREATOR_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.CREATOR_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.CREATOR_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.CREATOR_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LAST_MODIFIER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LAST_MODIFIER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LAST_MODIFIER_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LAST_MODIFIER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_START_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_START_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_TABLE

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
	const val LAST_EDITOR_JOIN =
		"LEFT JOIN $USER_TABLE $LAST_EDITOR_TABLE ON t.$LAST_MODIFIER_ID = $LAST_EDITOR_TABLE.$ID"

	const val LINKED_PROJECT_TABLE = "project_tb"
	const val SELECT_LINKED_PROJECT = """
        $LINKED_PROJECT_TABLE.$ID AS $LINKED_PROJECT_ID,
        $LINKED_PROJECT_TABLE.$PROJECT_NAME AS $LINKED_PROJECT_NAME,
        $LINKED_PROJECT_TABLE.$PROJECT_BEGIN_DATE AS $LINKED_PROJECT_START_DATE,
        $LINKED_PROJECT_TABLE.$PROJECT_BEGIN_TIME AS $LINKED_PROJECT_START_TIME,
        $LINKED_PROJECT_TABLE.$PROJECT_END_DATE AS $LINKED_PROJECT_END_DATE,
        $LINKED_PROJECT_TABLE.$PROJECT_END_TIME AS $LINKED_PROJECT_END_TIME,
        $LINKED_PROJECT_TABLE.$PROJECT_OPTIONS AS $LINKED_PROJECT_OPTIONS,
        $LINKED_PROJECT_TABLE.$VISIBLE AS $LINKED_PROJECT_VISIBLE
    """
	const val PROJECT_JOIN =
		"INNER JOIN $PROJECT_TABLE $LINKED_PROJECT_TABLE ON t.$LINKED_PROJECT_ID = $LINKED_PROJECT_TABLE.$ID AND $LINKED_PROJECT_TABLE.$VISIBLE IS TRUE"

	const val VISIBLE_CLAUSE = "(:visibilitySearched IS NULL OR t.$VISIBLE = :visibilitySearched)"

	const val PROJECT_CLAUSE = "(t.$LINKED_PROJECT_ID = :projectId)"
}
