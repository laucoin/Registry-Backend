package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isInRange

data class ProjectModel(
	var name: String? = null,
	var status: AvailabilityStatusEnum? = null,
	var begin: CustomDateTimeModel? = null,
	var end: CustomDateTimeModel? = null,
	var options: List<ProjectOptionEnum>? = emptyList(),
): GenericModel() {
	// Profil de l'utilisateur courant sur ce projet, renseigné uniquement par les lectures orientées utilisateur
	var profile: ProjectProfileModel? = null
	var favorite: Boolean = false

	var counts: ProjectCountsModel? = null

	fun isNotInRange(dateTime: CustomDateTimeModel?): Boolean {
		return dateTime.isInRange(begin, end).not()
	}
}
