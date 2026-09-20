package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum

data class SortModel<T>(
	val field: T,
	val direction: SortDirectionEnum,
)
