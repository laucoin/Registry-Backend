package fr.laucoin.registry.backend.domain.model

import java.util.UUID


data class UserSearchParamModel(
	var isVisible: Boolean? = null,
	var role: String? = null,
	var sort: SortModel? = null,
) {
	var query: String? = null

	var memberOfProjectId: UUID? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		role: String? = null,
		sort: SortModel? = null,
	): this(isVisible, role?.takeUnless { it.isBlank() }, sort = sort) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"firstName", "lastName", "email", "role", "birthday", "lastLogin",
		)
	}
}
