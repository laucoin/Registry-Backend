package fr.laucoin.registry.backend.domain.model

data class UserSearchParamModel(
    var textSearched: String? = null,
    var visibilitySearched: Boolean? = null,
)
