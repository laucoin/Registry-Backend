package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER
import org.jooq.Condition
import org.jooq.TableField
import org.jooq.impl.DSL

object GenericJooqQueries {
	fun creatorTable(): TbUser = TB_USER.`as`("create_tb")

	fun editorTable(): TbUser = TB_USER.`as`("editor_tb")

	fun projectTable(): TbProject = TB_PROJECT.`as`("project_tb")

	fun visibleCondition(field: TableField<*, Boolean?>, visibilitySearched: Boolean?): Condition =
		visibilitySearched?.let { field.eq(it) } ?: DSL.noCondition()
}
