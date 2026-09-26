package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.role.RoleEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_ROLE_PERMISSION
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER_ROLE_PERMISSION
import org.jooq.DSLContext
import org.jooq.impl.DSL.arrayAgg
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class RoleJooqRepository(private val dsl: DSLContext) {
	fun findUserRoles(): Flux<RoleEntity> = Flux.from(
		dsl.select(TB_USER_ROLE.NAME, TB_USER_ROLE.LEVEL, arrayAgg(TB_USER_ROLE_PERMISSION.PERMISSION))
			.from(TB_USER_ROLE_PERMISSION)
			.join(TB_USER_ROLE).on(TB_USER_ROLE.NAME.eq(TB_USER_ROLE_PERMISSION.ROLE))
			.groupBy(TB_USER_ROLE.NAME, TB_USER_ROLE.LEVEL)
			.orderBy(TB_USER_ROLE.NAME)
	).map { RoleEntity(role = it.value1()!!, level = it.value2()!!, permissions = it.value3()?.filterNotNull() ?: emptyList()) }

	fun findProjectRoles(): Flux<RoleEntity> = Flux.from(
		dsl.select(TB_PROJECT_ROLE.NAME, TB_PROJECT_ROLE.LEVEL, arrayAgg(TB_PROJECT_ROLE_PERMISSION.PERMISSION))
			.from(TB_PROJECT_ROLE_PERMISSION)
			.join(TB_PROJECT_ROLE).on(TB_PROJECT_ROLE.NAME.eq(TB_PROJECT_ROLE_PERMISSION.ROLE))
			.groupBy(TB_PROJECT_ROLE.NAME, TB_PROJECT_ROLE.LEVEL)
			.orderBy(TB_PROJECT_ROLE.NAME)
	).map { RoleEntity(role = it.value1()!!, level = it.value2()!!, permissions = it.value3()?.filterNotNull() ?: emptyList()) }
}
