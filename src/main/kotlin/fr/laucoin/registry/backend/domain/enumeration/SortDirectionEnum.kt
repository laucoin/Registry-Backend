package fr.laucoin.registry.backend.domain.enumeration

/**
 * The direction a v2 sorted collection is read in. It is a parameter of its
 * own (`direction=ASC|DESC`) rather than a per-field prefix, so `sort` only
 * ever names fields and never carries ordering syntax a caller has to learn.
 */
enum class SortDirectionEnum {
	ASC,
	DESC;

	val descending: Boolean
		get() = this == DESC

	companion object {
		val DEFAULT = ASC
	}
}
