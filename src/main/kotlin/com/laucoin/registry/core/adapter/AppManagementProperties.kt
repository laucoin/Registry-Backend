package com.laucoin.registry.core.adapter

import com.laucoin.registry.core.model.event.EventAuthorityEnum
import com.laucoin.registry.core.model.user.UserAuthorityEnum
import com.laucoin.registry.core.model.user.UserModel
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

@ConfigurationProperties(prefix = "registry.feature.app-management")
data class AppManagementProperties(
    private val supportEmail: String,
    private val userAuthority: HashMap<String, List<UserAuthorityEnum>>,
    private val eventAuthority: HashMap<String, List<EventAuthorityEnum>>,
) {
    private val uuid = UUID.fromString("3ca811bf-1677-4008-ac11-6e767c44d3c1")
    private val dateTime = LocalDateTime.of(1998, 10, 13, 0, 0)

    fun serviceAccount(): UserModel = UserModel(
        lastName = "service_account",
        email = supportEmail,
    ).apply {
        id = uuid
        creationDate = dateTime
        creatorId = uuid
        creatorLastName = lastName
        creatorEmail = email
        creatorVisible = true
        editionDate = dateTime
        editorId = uuid
        editorLastName = lastName
        editorEmail = email
        editorVisible = true
        visible = true
    }

    fun getLeastUserRole(): String = userAuthority.keys.last()

    fun getAuthorities(user: UserModel): Collection<GrantedAuthority> {
        var authorities: Collection<GrantedAuthority> = getUserAuthorities(user.role, user.id !!)

        if (Objects.nonNull(user.defaultProfileId)) {
            authorities += getEventAuthorities(user.defaultProfileRole, user.defaultProfileEventId !!)
        }

        return authorities
    }

    private fun getEventAuthorities(role: String?, eventId: UUID): Collection<GrantedAuthority> =
        eventAuthority[role]?.map { SimpleGrantedAuthority("${it.name}_${eventId}") } ?: emptyList()

    private fun getUserAuthorities(role: String?, userId: UUID): Collection<GrantedAuthority> =
        userAuthority[role]?.map {
            if (it === UserAuthorityEnum.ROLE_REGISTRY_USER) SimpleGrantedAuthority("${it.name}_${userId}")
            else SimpleGrantedAuthority(it.name)
        } ?: emptyList()
}
