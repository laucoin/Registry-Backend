package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import java.time.ZonedDateTime

data class EventProfileModel(
    var user: UserModel? = null,
    var role: String? = null,
    var status: ProfileStatusEnum? = INVITED,
    var startAccess: ZonedDateTime? = null,
    var endAccess: ZonedDateTime? = null,
): GenericEventModel() {
    override fun getSearchableValues(): List<String> = user?.getSearchableValues().orEmpty() + event?.getSearchableValues().orEmpty()
}
