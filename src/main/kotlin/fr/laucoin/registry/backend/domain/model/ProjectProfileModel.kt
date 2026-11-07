package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED

data class ProjectProfileModel(
	var user: UserModel? = null,
	// Résumé du projet, chargé uniquement pour les lectures orientées utilisateur (invitations, profils de l'utilisateur courant)
	var project: ProjectModel? = null,
	var role: String? = null,
	var availabilityStatus: AvailabilityStatusEnum? = null,
	var status: ProfileStatusEnum? = INVITED,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
): GenericProjectModel()
