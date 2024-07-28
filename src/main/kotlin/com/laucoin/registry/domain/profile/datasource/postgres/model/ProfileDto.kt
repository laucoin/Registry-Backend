package com.laucoin.registry.domain.profile.datasource.postgres.model

import com.laucoin.registry.core.datasource.postgres.model.util.GenericEventDto
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileEndAccessField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileStartAccessField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileTable
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserDefaultProfileIdField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserEmailField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserFirstNameField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserIdField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserLastNameField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserOidcIdField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserRoleField
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(profileTable)
data class ProfileDto(
    @Column(profileUserIdField)
    var userId: UUID? = null,
    @Column(profileUserOidcIdField)
    var userOidcId: UUID? = null,
    @Column(profileUserFirstNameField)
    var userFirstName: String? = null,
    @Column(profileUserLastNameField)
    var userLastName: String? = null,
    @Column(profileUserEmailField)
    var userEmail: String? = null,
    @Column(profileUserRoleField)
    var userRole: String? = null,
    @Column(profileUserDefaultProfileIdField)
    var userDefaultProfileId: UUID? = null,
    @Column(profileUserDefaultProfileIdField)
    var userVisible: Boolean? = null,
    var role: String? = null,
    var accepted: Boolean = false,
    @Column(profileStartAccessField)
    var startAccess: LocalDateTime? = null,
    @Column(profileEndAccessField)
    var endAccess: LocalDateTime? = null,
): GenericEventDto() {
    constructor(profile: ProfileModel): this() {
        userId = profile.userId
        eventId = profile.eventId
        role = profile.role
        accepted = profile.accepted
        startAccess = profile.startAccess
        endAccess = profile.endAccess
        populateGenericDto(profile)
    }

    fun toModel(): EnrichedProfileModel {
        val profile = EnrichedProfileModel()
        profile.let {
            it.userId = userId
            it.eventId = eventId
            it.role = role
            it.accepted = accepted
            it.startAccess = startAccess
            it.endAccess = endAccess

            if (Objects.nonNull(userId)) {
                it.user = UserModel()
                it.user !!.let { user ->
                    user.oidcId = userOidcId
                    user.firstName = userFirstName
                    user.lastName = userLastName
                    user.email = userEmail
                    user.role = userRole
                    user.defaultProfileId = userDefaultProfileId
                    user.visible = userVisible ?: visible
                }
            }

            populateGenericModel(element = it)
        }
        return profile
    }
}
