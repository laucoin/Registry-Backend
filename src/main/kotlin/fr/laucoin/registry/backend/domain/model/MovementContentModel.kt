package fr.laucoin.registry.backend.domain.model

import java.util.UUID

data class MovementContentModel(
    var participant: ParticipantModel? = null,
    var movementId: UUID? = null,
): GenericModel() {
    override fun getSearchableValues(): List<String> = participant?.getSearchableValues() ?: emptyList()
}
