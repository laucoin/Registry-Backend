package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID

interface IRoleService {
    fun getLevelByUserRole(role: String?): Int?
    fun getLevelByEventRole(role: String): Int
    fun getLevel0RoleFromEventRoles(): String
    fun getDefaultUserRole(): String?
    fun getAuthoritiesByUserRole(role: String?): List<String>
    fun getAuthoritiesByEventRole(role: String, eventId: UUID): List<String>
    fun getEventIdFromCurrentUserProfiles(currentUser: CurrentUserModel): List<UUID>
    fun getAssignableUserRoles(currentUser: UserModel): List<String>
    fun getAssignableEventRoles(profile: EventProfileModel): List<String>
}
