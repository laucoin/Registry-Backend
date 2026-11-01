package fr.laucoin.registry.backend.domain.model

data class SortModel(
	val field: String,
	val isDescending: Boolean = false,
)
