package fr.laucoin.registry.backend.domain.model

/**
 * One sort key of the API v2 sort grammar (`sort=field,otherField`).
 * The field is a per-resource enum so only whitelisted fields ever reach a
 * query — free-form sort input never crosses the port boundary.
 */
data class SortModel<T>(
	val field: T,
	val descending: Boolean = false,
)
