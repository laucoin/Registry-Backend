package fr.laucoin.registry.backend.domain.model

data class MovementContentModel(
    var participant: ParticipantModel? = null,
): GenericModel() {
    override fun getSearchableValues(): List<String> = participant?.getSearchableValues() ?: emptyList()
}
