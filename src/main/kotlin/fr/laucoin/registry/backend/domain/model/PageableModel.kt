package fr.laucoin.registry.backend.domain.model

data class PageableModel(
	var offset: Long,
	var limit: Int,
)
