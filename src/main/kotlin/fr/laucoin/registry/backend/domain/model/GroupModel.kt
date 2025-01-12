package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.util.UUID

data class GroupModel(
    var name: String? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
    var members: List<ParticipantModel> = emptyList(),
): GenericEventModel() {
    override fun getSearchableValues(): List<String> =
        event?.getSearchableValues().orEmpty() + members.flatMap { it.getSearchableValues() }

    fun getNewMembers(group: GroupModel): List<ParticipantModel> {
        val currentParticipants = members.mapNotNull { it.id }
        return group.members
            .filter { ! currentParticipants.contains(it.id) }
    }

    fun getNewMemberIds(group: GroupModel): List<UUID> {
        return getNewMembers(group).mapNotNull(ParticipantModel::id)
    }

    fun getNewMemberIds(newMembers: List<UUID>): List<UUID> {
        val currentParticipants = members.mapNotNull { it.id }
        return newMembers.filter { ! currentParticipants.contains(it) }
    }

    fun getRemovedMemberIds(group: GroupModel): List<UUID> {
        val newParticipants = group.members.mapNotNull { it.id }
        return members
            .filter { ! newParticipants.contains(it.id) }
            .mapNotNull(ParticipantModel::id)
    }
}
