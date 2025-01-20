package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import java.time.ZonedDateTime
import java.util.UUID

data class MovementModel(
    var dateTime: ZonedDateTime = ZonedDateTime.now(),
    var type: MovementTypeEnum? = null,
    var content: List<MovementContentModel> = emptyList(),
): GenericEventModel() {
    data class MovementContentModel(
        var participant: ParticipantModel? = null,
    ): GenericModel() {
        override fun getSearchableValues(): List<String> = participant?.getSearchableValues() ?: emptyList()
    }

    override fun getSearchableValues(): List<String> =
        event?.getSearchableValues().orEmpty() + content.flatMap { it.getSearchableValues() }

    fun getNewContent(movement: MovementModel): List<MovementContentModel> {
        val currentParticipants = content.mapNotNull { it.participant?.id }
        return movement.content
            .filter { ! currentParticipants.contains(it.participant?.id) }
    }

    fun getNewContentParticipantIds(movement: MovementModel): List<UUID> {
        return getNewContent(movement).mapNotNull { it.participant?.id }
    }

    fun getRemovedContentParticipantIds(movement: MovementModel): List<UUID> {
        val newParticipants = movement.content.mapNotNull { it.participant?.id }
        return content
            .filter { ! newParticipants.contains(it.participant?.id) }
            .mapNotNull { it.participant?.id }
    }
}
