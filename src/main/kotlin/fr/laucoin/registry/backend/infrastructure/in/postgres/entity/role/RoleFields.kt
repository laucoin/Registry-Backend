package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.role

object RoleFields {
	const val USER_ROLE_PERMISSION_TABLE = "tb_user_role_permission"
	const val USER_ROLE_TABLE = "tb_user_role"

	const val PROJECT_ROLE_PERMISSION_TABLE = "tb_project_role_permission"
	const val PROJECT_ROLE_TABLE = "tb_project_role"

	const val ROLE_PERMISSIONS = "permissions"
	const val ROLE_PERMISSION = "permission"
	const val ROLE_NAME = "role"
	const val ENTITY_ROLE_NAME = "name"
	const val ROLE_LEVEL = "level"
}
