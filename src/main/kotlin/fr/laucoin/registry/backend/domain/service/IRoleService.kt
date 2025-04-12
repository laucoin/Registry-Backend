package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import java.util.UUID

interface IRoleService {
    fun getLevelByUserRole(role: String?): Int?
    fun getLevelByEventRole(role: String): Int
    fun getLevel0RoleFromEventRoles(): String
    fun getDefaultUserRole(): String?
    fun getAuthoritiesByUserRole(role: String?): List<String>
    fun getAuthoritiesByEventRole(role: String, eventId: UUID, visibility: Boolean?): List<String>
    fun getOptionAuthoritiesByEvent(eventId: UUID, eventOptions: List<EventOptionEnum>): List<String>
    fun getEventIdsFromCurrentUserProfiles(currentUser: CurrentUserModel): List<UUID>
    fun getAssignableUserRoles(currentUser: CurrentUserModel): List<String>
    fun getAssignableEventRoles(profile: EventProfileModel): List<String>
}
