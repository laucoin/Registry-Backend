package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role

object RoleFields {
    const val USER_ROLE_PERMISSION_TABLE = "tb_user_role_permission"
    const val USER_ROLE_TABLE = "tb_user_role"

    const val EVENT_ROLE_PERMISSION_TABLE = "tb_event_role_permission"
    const val EVENT_ROLE_TABLE = "tb_event_role"

    const val ROLE_PERMISSIONS = "permissions"
    const val ROLE_PERMISSION = "permission"
    const val ROLE_NAME = "role"
    const val ENTITY_ROLE_NAME = "name"
    const val ROLE_LEVEL = "level"
}
