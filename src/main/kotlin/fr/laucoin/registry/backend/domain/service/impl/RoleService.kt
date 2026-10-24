package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_PREFIX
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.domain.port.IRolePort
import fr.laucoin.registry.backend.domain.service.IRoleService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Objects
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

	override fun getLevelByProjectRole(role: String): Int = projectRoles[role]?.first ?: 0

	override fun getLevel0RoleFromProjectRoles(): String =
		projectRoles.entries.firstOrNull { it.value.first == 0 }?.key ?: error("No level 0 project role is defined")

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
		} else userRoles[role]?.second ?: emptyList()
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
		val roleLevel: Int? = roles[role]?.first
		if (Objects.isNull(roleLevel)) {
			return emptyList()
		}
		val eligibleRoles = roles.filter { it.value.first > roleLevel!! }.keys.toMutableList()
		eligibleRoles.add(role!!)
		return eligibleRoles
	}
}
