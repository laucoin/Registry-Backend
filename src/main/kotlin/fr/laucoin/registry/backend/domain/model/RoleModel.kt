package fr.laucoin.registry.backend.domain.model

data class RoleModel(
	var role: String,
	var level: Int,
	var permissions: List<String>
)
