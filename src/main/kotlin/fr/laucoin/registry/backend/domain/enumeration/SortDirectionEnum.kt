package fr.laucoin.registry.backend.domain.enumeration

/**
 * The direction a sorted collection is read in (API v2 sort grammar, ADR 017).
 * It is a parameter of its own rather than a prefix on each field: `sort` names
 * WHAT to order by, `direction` says WHICH WAY, and a caller no longer has to
 * know that a hyphen is meaningful to reverse a list.
 */
enum class SortDirectionEnum {
	ASC,
	DESC;

	val descending: Boolean
		get() = this == DESC

	companion object {
		val DEFAULT = ASC

		fun fromParamName(paramName: String): SortDirectionEnum? =
			entries.firstOrNull { it.name.equals(paramName, ignoreCase = true) }
	}
}
