package fr.laucoin.registry.backend.domain.model

import java.util.UUID

data class PreferencesModel(
    var userId: UUID? = null,
    var selectedProfile: EventProfileModel? = null
): GenericModel() {
    override fun getSearchableValues(): List<String> = listOfNotNull()
}
