package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class ParticipantModel(
	var firstName: String? = null,
	var lastName: String? = null,
	var birthday: LocalDate? = null,
	var type: ParticipantTypeEnum? = null,
	var groups: List<GroupModel> = emptyList(),
	var availableGroups: List<GroupModel> = emptyList(),
	var status: PresenceStatusEnum? = null,
	var availabilityWarning: Boolean = false,
	var lastMovement: ZonedDateTime? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
	var departedAt: ZonedDateTime? = null,
	var user: UserModel? = null,
) : GenericProjectModel() {
	fun isNotUsable() = isNotVisible()

	fun getNewGroups(participant: ParticipantModel): List<GroupModel> {
		val currentGroups = groups.mapNotNull { it.id }
		return participant.groups.filter { !currentGroups.contains(it.id) }
	}

	fun getNewGroupIds(participant: ParticipantModel): List<UUID> {
		return getNewGroups(participant).mapNotNull(GroupModel::id)
	}

	fun getOldGroupIds(participant: ParticipantModel): List<UUID> {
		val newGroups = participant.groups.mapNotNull { it.id }
		return groups
			.filter { !newGroups.contains(it.id) }
			.mapNotNull(GroupModel::id)
	}
}
