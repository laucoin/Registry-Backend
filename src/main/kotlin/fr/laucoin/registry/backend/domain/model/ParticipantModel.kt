package fr.laucoin.registry.backend.domain.model

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class ParticipantModel(
    var firstName: String? = null,
    var lastName: String? = null,
    var birthday: LocalDate? = null,
    var groups: List<GroupModel> = emptyList(),
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
    var user: UserModel? = null,
    var purged: Boolean? = null,
): GenericEventModel() {
    override fun getSearchableValues(): List<String> = listOfNotNull(firstName, lastName)

    fun getNewGroups(participant: ParticipantModel): List<GroupModel> {
        val currentGroups = groups.mapNotNull { it.id }
        return participant.groups
            .filter { ! currentGroups.contains(it.id) }
    }

    fun getNewGroupIds(participant: ParticipantModel): List<UUID> {
        return getNewGroups(participant).mapNotNull(GroupModel::id)
    }

    fun getRemovedGroupIds(participant: ParticipantModel): List<UUID> {
        val newGroups = participant.groups.mapNotNull { it.id }
        return groups
            .filter { ! newGroups.contains(it.id) }
            .mapNotNull(GroupModel::id)
    }
}
