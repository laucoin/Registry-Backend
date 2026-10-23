package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED

data class ProjectProfileModel(
	var user: UserModel? = null,
	// Project summary, only loaded for user-oriented reads (invitations, current user profiles)
	var project: ProjectModel? = null,
	var role: String? = null,
	var availabilityStatus: AvailabilityStatusEnum? = null,
	var status: ProfileStatusEnum? = INVITED,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
): GenericProjectModel()
