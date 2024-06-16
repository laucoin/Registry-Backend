package com.laucoin.registry.domain.profile.model

import com.laucoin.registry.core.model.event.EventAuthorityEnum
import com.laucoin.registry.core.model.user.UserModel

data class EnrichedProfileModel(
    var user: UserModel? = null,
    var authorities: List<EventAuthorityEnum>? = null,
): ProfileModel() {
    override fun filterFields(): List<String?> = super.filterFields() + (user?.filterFields() ?: emptyList())
}
