package fr.laucoin.registry.backend.domain.service.support.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_PREFIX
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_RESTORE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.domain.port.IRolePort
import fr.laucoin.registry.backend.domain.service.LoggerService
import fr.laucoin.registry.backend.domain.service.support.IRoleService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Objectif : Détient en mémoire les rôles et leurs permissions.
 * Périmètre : Charge les tables de rôles au démarrage et résout les niveaux, les rôles attribuables et les autorités.
 * Limites : Les rôles sont des données chargées une fois ; un changement nécessite un redémarrage.
 */
@Service
class RoleService(
	private val port: IRolePort,
	@param:Value($$"${registry.security.default-role}")
	private val defaultUserRole: String,
) : ApplicationListener<ContextRefreshedEvent>, IRoleService, LoggerService() {
	private val hiddenProjectAuthorities =
		setOf(REGISTRY_PROJECT_R, REGISTRY_PROJECT_U, REGISTRY_PROJECT_D, REGISTRY_PROJECT_RESTORE)

	private val uuidRegex: Regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

	@Volatile
	private var userRoles: Map<String, Pair<Int, List<String>>> = emptyMap()

	@Volatile
	private var projectRoles: Map<String, Pair<Int, List<String>>> = emptyMap()

	// Le blocage est voulu ici : cela s'exécute une seule fois sur le thread de rafraîchissement du contexte
	override fun onApplicationEvent(event: ContextRefreshedEvent) {
		Mono.zip(loadRoles(port.findUserRoles()), loadRoles(port.findProjectRoles()))
			.doOnNext { userRoles = it.t1; projectRoles = it.t2 }
			.block()
	}

	private fun loadRoles(roles: Flux<RoleModel>): Mono<Map<String, Pair<Int, List<String>>>> =
		roles.collectMap({ it.role }, { Pair(it.level, it.permissions) })

	override fun getLevelByUserRole(role: String?): Int? = userRoles[role]?.first

	override fun getLevel0RoleFromProjectRoles(): String =
		projectRoles.entries.firstOrNull { it.value.first == 0 }?.key ?: error("No level 0 project role is defined")

	override fun getDefaultUserRole(): String? {
		if (userRoles.containsKey(defaultUserRole)) return defaultUserRole
		log.warn("Default user role not found")
		return null
	}

	override fun getAuthoritiesByUserRole(role: String?): List<String> {
		val authorities = userRoles[role]?.second
		if (authorities == null) log.warn("User role \"{}\" not found in \"{}\"", role, userRoles.keys)
		return authorities.orEmpty()
	}

	override fun getAuthoritiesByProjectRole(role: String, projectId: UUID, visibility: Boolean?): List<String> {
		val (level, authorities) = projectRoles[role] ?: return unknownProjectRole(role)
		if (visibility == true) return authorities.map { "${projectId}_$it" }
		if (level != 0) return emptyList()
		return authorities.filter { it in hiddenProjectAuthorities }.map { "${projectId}_$it" }
	}

	private fun unknownProjectRole(role: String): List<String> {
		log.warn("Project role \"{}\" not found in \"{}\"", role, projectRoles.keys)
		return emptyList()
	}

	override fun getOptionAuthoritiesByProject(projectId: UUID, projectOptions: List<ProjectOptionEnum>): List<String> {
		return projectOptions.map { "${projectId}_${REGISTRY_PROJECT_OPTION_PREFIX}$it" }
	}

	override fun getProjectIdsFromCurrentUserProfiles(currentUser: CurrentUserModel): List<UUID> {
		return currentUser.authorities
			.mapNotNull { it.authority?.let { authority -> uuidRegex.find(authority)?.value } }
			.map { UUID.fromString(it) }.distinct()
	}

	override fun getAssignableUserRoles(currentUser: CurrentUserModel): List<String> {
		return findAssignableRoles(currentUser.role, userRoles)
	}

	override fun getAssignableProjectRoles(profile: ProjectProfileModel): List<String> {
		return findAssignableRoles(profile.role, projectRoles)
	}

	private fun findAssignableRoles(role: String?, roles: Map<String, Pair<Int, List<String>>>): List<String> {
		if (role == null) return emptyList()
		val roleLevel = roles[role]?.first ?: return emptyList()
		return roles.filterValues { it.first > roleLevel }.keys.toList() + role
	}
}
