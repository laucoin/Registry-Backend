package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.role

data class RoleEntity(
	var role: String,
	var level: Int,
	var permissions: List<String>
)
