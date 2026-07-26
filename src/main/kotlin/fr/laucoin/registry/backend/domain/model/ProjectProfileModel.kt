package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED

data class ProjectProfileModel(
	var user: UserModel? = null,
	var role: String? = null,
	var availabilityStatus: AvailabilityStatusEnum? = null,
	var status: ProfileStatusEnum? = INVITED,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
	var favorite: Boolean = false,
) : GenericProjectModel()
