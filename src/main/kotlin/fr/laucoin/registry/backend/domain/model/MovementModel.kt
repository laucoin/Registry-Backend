package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import java.time.ZonedDateTime

data class MovementModel(
    var dateTime: ZonedDateTime = ZonedDateTime.now(),
    var type: MovementTypeEnum? = null,
    var content: List<MovementContentModel> = emptyList(),
): GenericEventModel() {
    override fun getSearchableValues(): List<String> =
        event?.getSearchableValues().orEmpty() + content.flatMap { it.getSearchableValues() }

    fun changed(movement: MovementModel): Boolean = dateTime != movement.dateTime
    fun getNewContent(movement: MovementModel): List<MovementContentModel> {
        val currentParticipants = content.mapNotNull { it.participant?.id }
        return movement.content
            .filter { ! currentParticipants.contains(it.participant?.id) }
    }

    fun getRemovedContent(movement: MovementModel): List<MovementContentModel> {
        val newParticipants = movement.content.mapNotNull { it.participant?.id }
        return content
            .filter { ! newParticipants.contains(it.participant?.id) }
    }
}
