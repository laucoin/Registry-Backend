package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_D
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_OPTION_PREFIX
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_R
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_U
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.repository.IRoleModelRepository
import fr.laucoin.registry.backend.domain.service.IRoleService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service

@Service
class RoleService(
    private val repository: IRoleModelRepository,
    @Value("\${registry.security.default-role}")
    private val defaultUserRole: String,
): ApplicationListener<ContextRefreshedEvent>, IRoleService, LoggerService() {
    private val uuidRegex: Regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    private val userRoles: HashMap<String, Pair<Int, List<String>>> = hashMapOf()
    private val eventRoles: HashMap<String, Pair<Int, List<String>>> = hashMapOf()

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        repository.findUserRoles()
            .doOnNext { userRoles[it.role] = Pair(it.level, it.permissions) }
            .subscribe()

        repository.findEventRoles()
            .doOnNext { eventRoles[it.role] = Pair(it.level, it.permissions) }
            .subscribe()
    }

    override fun getLevelByUserRole(role: String?): Int? = userRoles[role]?.first

    override fun getLevelByEventRole(role: String): Int = eventRoles[role]?.first ?: 0

    override fun getLevel0RoleFromEventRoles(): String = eventRoles.filter { it.value.first == 0 }.keys.first()

    override fun getDefaultUserRole(): String? {
        val roles = userRoles.filter { it.key == defaultUserRole }.keys
        return if (roles.isEmpty()) {
            log.warn("Default user role not found")
            null
        } else roles.first()
    }

    override fun getAuthoritiesByUserRole(role: String?): List<String> {
        return if (! userRoles.containsKey(role)) {
            log.warn("User role \"{}\" not found in \"{}\"", role, userRoles.keys)
            emptyList()
        } else userRoles[role] !!.second
    }

    override fun getAuthoritiesByEventRole(role: String, eventId: UUID, visibility: Boolean?): List<String> {
        if (! eventRoles.containsKey(role)) {
            log.warn("Event role \"{}\" not found in \"{}\"", role, eventRoles.keys)
            return emptyList()
        }

        val roleAuthoritiesMapping = eventRoles[role]
        return when {
            Objects.isNull(roleAuthoritiesMapping) || (visibility != true && roleAuthoritiesMapping !!.first != 0) -> emptyList()
            visibility != true && roleAuthoritiesMapping !!.first == 0 ->
                roleAuthoritiesMapping.second.filter { listOf(REGISTRY_EVENT_R, REGISTRY_EVENT_U, REGISTRY_EVENT_D).contains(it) }
                    .map { "${eventId}_$it" }

            else -> roleAuthoritiesMapping !!.second.map { "${eventId}_$it" }
        }
    }

    override fun getOptionAuthoritiesByEvent(eventId: UUID, eventOptions: List<EventOptionEnum>): List<String> {
        return eventOptions.map { "${eventId}_${REGISTRY_EVENT_OPTION_PREFIX}$it" }
    }

    override fun getEventIdsFromCurrentUserProfiles(currentUser: CurrentUserModel): List<UUID> {
        return currentUser.authorities
            .mapNotNull { uuidRegex.find(it.authority)?.value }
            .map { UUID.fromString(it) }.distinct()
    }

    override fun getAssignableUserRoles(currentUser: CurrentUserModel): List<String> {
        return findAssignableRoles(currentUser.role, userRoles)
    }

    override fun getAssignableEventRoles(profile: EventProfileModel): List<String> {
        return findAssignableRoles(profile.role, eventRoles)
    }

    private fun findAssignableRoles(role: String?, roles: HashMap<String, Pair<Int, List<String>>>): List<String> {
        val roleLevel: Int? = roles[role]?.first
        if (Objects.isNull(roleLevel)) {
            return emptyList()
        }
        val eligibleRoles = roles.filter { it.value.first > roleLevel !! }.keys.toMutableList()
        eligibleRoles.add(role !!)
        return eligibleRoles
    }
}
