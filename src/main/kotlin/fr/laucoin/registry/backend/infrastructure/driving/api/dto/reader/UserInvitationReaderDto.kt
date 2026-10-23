package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

/** A user's profile seen from the user side: the project summary matters, the user is implicit. */
data class UserInvitationReaderDto(
	var project: InvitationProjectReaderDto? = null,
	var role: LabelDto? = null,
	var availabilityStatus: LabelDto? = null,
	var status: LabelDto? = null,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
): GenericProjectReaderDto() {
	data class InvitationProjectReaderDto(
		var id: java.util.UUID? = null,
		var name: String? = null,
		var begin: CustomDateTimeModel? = null,
		var end: CustomDateTimeModel? = null,
	)
}
