package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import java.util.UUID

data class GroupModel(
	var name: String? = null,
	var status: AvailabilityStatusEnum? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
	var membersCount: Long? = null,
	var insideMembersCount: Long? = null,
	var outsideMembersCount: Long? = null,
	var members: List<ParticipantModel> = emptyList(),
): GenericProjectModel() {
	fun getNewMembers(group: GroupModel): List<ParticipantModel> {
		val currentParticipants = members.mapNotNull { it.id }
		return group.members
			.filter { !currentParticipants.contains(it.id) }
	}

	fun getNewMemberIds(group: GroupModel): List<UUID> {
		return getNewMembers(group).mapNotNull(ParticipantModel::id)
	}

	fun getNewMemberIds(newMembers: List<UUID>): List<UUID> {
		val currentParticipants = members.mapNotNull { it.id }
		return newMembers.filter { !currentParticipants.contains(it) }
	}

	fun getOldMemberIds(group: GroupModel): List<UUID> {
		val newParticipants = group.members.mapNotNull { it.id }
		return members
			.filter { !newParticipants.contains(it.id) }
			.mapNotNull(ParticipantModel::id)
	}
}
