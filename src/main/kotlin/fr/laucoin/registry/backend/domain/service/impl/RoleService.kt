package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_PREFIX
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.port.IRolePort
import fr.laucoin.registry.backend.domain.service.IRoleService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service

@Service
class RoleService(
	private val port: IRolePort,
	@param:Value("\${registry.security.default-role}")
	private val defaultUserRole: String,
): ApplicationListener<ContextRefreshedEvent>, IRoleService, LoggerService() {
	private val uuidRegex: Regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

	private val userRoles: HashMap<String, Pair<Int, List<String>>> = hashMapOf()
	private val projectRoles: HashMap<String, Pair<Int, List<String>>> = hashMapOf()

	override fun onApplicationEvent(event: ContextRefreshedEvent) {
		port.findUserRoles()
			.doOnNext { userRoles[it.role] = Pair(it.level, it.permissions) }
			.subscribe()

		port.findProjectRoles()
			.doOnNext { projectRoles[it.role] = Pair(it.level, it.permissions) }
			.subscribe()
	}

	override fun getLevelByUserRole(role: String?): Int? = userRoles[role]?.first

	override fun getLevelByProjectRole(role: String): Int = projectRoles[role]?.first ?: 0

	override fun getLevel0RoleFromProjectRoles(): String = projectRoles.filter { it.value.first == 0 }.keys.first()

	override fun getDefaultUserRole(): String? {
		val roles = userRoles.filter { it.key == defaultUserRole }.keys
		return if (roles.isEmpty()) {
			log.warn("Default user role not found")
			null
		} else roles.first()
	}

	override fun getAuthoritiesByUserRole(role: String?): List<String> {
		return if (!userRoles.containsKey(role)) {
			log.warn("User role \"{}\" not found in \"{}\"", role, userRoles.keys)
			emptyList()
		} else userRoles[role]!!.second
	}

	override fun getAuthoritiesByProjectRole(role: String, projectId: UUID, visibility: Boolean?): List<String> {
		val roleAuthoritiesMapping = projectRoles[role]
		return when {
			Objects.isNull(roleAuthoritiesMapping) -> {
				log.warn("Project role \"{}\" not found in \"{}\"", role, projectRoles.keys)
				emptyList()
			}

			visibility != true && roleAuthoritiesMapping!!.first != 0 -> emptyList()
			visibility != true ->
				roleAuthoritiesMapping!!.second.filter {
					listOf(
						REGISTRY_PROJECT_R,
						REGISTRY_PROJECT_U,
						REGISTRY_PROJECT_D
					).contains(it)
				}.map { "${projectId}_$it" }

			else -> roleAuthoritiesMapping!!.second.map { "${projectId}_$it" }
		}
	}

	override fun getOptionAuthoritiesByProject(projectId: UUID, projectOptions: List<ProjectOptionEnum>): List<String> {
		return projectOptions.map { "${projectId}_${REGISTRY_PROJECT_OPTION_PREFIX}$it" }
	}

	override fun getProjectIdsFromCurrentUserProfiles(currentUser: CurrentUserModel): List<UUID> {
		return currentUser.authorities
			.mapNotNull { uuidRegex.find(it.authority)?.value }
			.map { UUID.fromString(it) }.distinct()
	}

	override fun getAssignableUserRoles(currentUser: CurrentUserModel): List<String> {
		return findAssignableRoles(currentUser.role, userRoles)
	}

	override fun getAssignableProjectRoles(profile: ProjectProfileModel): List<String> {
		return findAssignableRoles(profile.role, projectRoles)
	}

	private fun findAssignableRoles(role: String?, roles: HashMap<String, Pair<Int, List<String>>>): List<String> {
		val roleLevel: Int? = roles[role]?.first
		if (Objects.isNull(roleLevel)) {
			return emptyList()
		}
		val eligibleRoles = roles.filter { it.value.first > roleLevel!! }.keys.toMutableList()
		eligibleRoles.add(role!!)
		return eligibleRoles
	}
}
