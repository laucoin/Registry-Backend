package com.laucoin.registry.core.adapter

import com.laucoin.registry.core.model.event.EventAuthorityEnum
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserAuthorityEnum
import com.laucoin.registry.core.model.util.HistoryModel
import com.laucoin.registry.core.model.util.HistoryUserModel
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "registry.feature.app-management")
data class SecurityProperties(
    private val email: String,
    private val firstName: String,
    private val lastName: String,
    private val userAuthority: HashMap<String, List<UserAuthorityEnum>>,
    private val eventAuthority: HashMap<String, List<EventAuthorityEnum>>,
) {
    private val uuid = UUID.fromString("3ca811bf-1677-4008-ac11-6e767c44d3c1")
    private val dateTime = LocalDateTime.of(1998, 10, 13, 0, 0)

    fun serviceAccount(): EnrichedUserModel {
        val history = HistoryModel(
            date = dateTime,
            user = HistoryUserModel(
                id = uuid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                visible = true,
            )
        )

        val serviceAccount = EnrichedUserModel()
        serviceAccount.let {
            it.id = uuid
            it.firstName = firstName
            it.lastName = lastName
            it.email = email
            it.creation = history
            it.edition = history
            it.visible = true
        }
        return serviceAccount
    }

    fun userRoles(): List<String> = userAuthority.keys.toList()
    fun profileRoles(): List<String> = eventAuthority.keys.toList()
    fun userAuthorities(role: String?): List<UserAuthorityEnum> = userAuthority[role] ?: emptyList()
    fun eventAuthorities(role: String?): List<EventAuthorityEnum> = eventAuthority[role] ?: emptyList()

    fun userManagerRoles(): List<String> =
        userAuthority.filter {
            it.value.any { authority -> authority == UserAuthorityEnum.ROLE_REGISTRY_GLOBAL_USER_MANAGEMENT }
        }.keys.toList()
}
