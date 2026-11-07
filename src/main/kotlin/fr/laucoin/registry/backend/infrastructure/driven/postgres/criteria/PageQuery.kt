package fr.laucoin.registry.backend.infrastructure.driven.postgres.criteria

/** Pagination et tri liés aux requêtes SQL par `:#{#page.…}` ; un `limit` seul sert aux recherches bornées. */
data class PageQuery(
	val limit: Int,
	val offset: Long = 0,
	val sortField: String? = null,
	val isDescending: Boolean? = null,
)
