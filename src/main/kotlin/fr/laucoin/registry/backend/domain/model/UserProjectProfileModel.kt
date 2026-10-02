package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED

/**
 * A caller's own Profile decorated with its full Project — used by [IUserProjectProfileService],
 * whose endpoints are not scoped under a Project id, so the Project has to travel in the response.
 * Carries the same fields as [ProjectProfileModel] (not a subtype of it: the two travel through
 * different persistence paths) plus [project].
 */
data class UserProjectProfileModel(
	var user: UserModel? = null,
	var role: String? = null,
	var availabilityStatus: AvailabilityStatusEnum? = null,
	var status: ProfileStatusEnum? = INVITED,
	var startAccess: CustomDateTimeModel? = null,
	var endAccess: CustomDateTimeModel? = null,
	var isFavorite: Boolean = false,
	var project: ProjectModel? = null,
): GenericModel()
