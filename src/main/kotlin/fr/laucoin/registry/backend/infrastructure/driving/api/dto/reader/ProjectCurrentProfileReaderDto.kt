package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto

/** Le profil de l'utilisateur courant sur un projet : l'utilisateur est implicite, donc ni l'utilisateur ni le projet ne sont répétés. */
data class ProjectCurrentProfileReaderDto(
	var role: LabelDto? = null,
	var availabilityStatus: LabelDto? = null,
	var status: LabelDto? = null,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
): GenericReaderDto()
