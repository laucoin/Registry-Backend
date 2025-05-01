package fr.laucoin.registry.backend.domain.model

import java.util.UUID

data class PreferencesModel(
    var userId: UUID? = null,
    var selectedProfile: ProjectProfileModel? = null
): GenericModel()
