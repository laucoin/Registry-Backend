package com.laucoin.registry.core.model.user

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY
import com.laucoin.registry.core.config.GsonConfig
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import java.util.Objects

data class EnrichedUserModel(
    @JsonProperty(access = READ_ONLY)
    var defaultProfile: ProfileModel? = null,
    var profiles: List<EnrichedProfileModel>? = null,
    var authorities: List<UserAuthorityEnum>? = null,
): UserModel() {
    fun defaultProfileDisable(): Boolean = Objects.nonNull(defaultProfile?.visible) && ! defaultProfile !!.visible

    override fun toString(): String {
        return GsonConfig().gson().toJson(this)
    }
}
