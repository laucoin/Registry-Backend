package fr.laucoin.registry.backend.domain.model

data class UserSearchParamModel(
	var isVisible: Boolean? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
	): this(isVisible) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}

