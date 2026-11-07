package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import java.util.UUID

/** Le profil d'un utilisateur vu côté utilisateur : le résumé du projet compte, l'utilisateur est implicite. */
data class UserInvitationReaderDto(
	var project: InvitationProjectReaderDto? = null,
	var role: LabelDto? = null,
	var availabilityStatus: LabelDto? = null,
	var status: LabelDto? = null,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
): GenericReaderDto() {
	data class InvitationProjectReaderDto(
		var id: UUID? = null,
		var name: String? = null,
		var begin: CustomDateTimeModel? = null,
		var end: CustomDateTimeModel? = null,
		var favorite: Boolean = false,
	)
}
