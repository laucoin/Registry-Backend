package fr.laucoin.registry.backend.domain.model

data class UserSearchParamModel(
	var visibilitySearched: Boolean? = null,
) {
	var textSearched: String? = null

	constructor(
		textSearched: String? = null,
		visibilitySearched: Boolean? = null,
	) : this(visibilitySearched) {
		this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
	}
}

