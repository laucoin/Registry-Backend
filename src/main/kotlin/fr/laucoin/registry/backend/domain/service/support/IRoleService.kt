package fr.laucoin.registry.backend.domain.service.support

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import java.util.UUID

/**
 * Objectif : Contrat de résolution des rôles, niveaux et autorités.
 * Périmètre : Déclare les autorités d'un rôle, les rôles assignables et les projets d'un utilisateur courant.
 * Limites : Contrat uniquement ; pas de révocation en cours de session, les autorités sont recalculées à la conversion du jeton.
 */
interface IRoleService {
	fun getLevelByUserRole(role: String?): Int?
	fun getLevel0RoleFromProjectRoles(): String
	fun getDefaultUserRole(): String?
	fun getAuthoritiesByUserRole(role: String?): List<String>
	fun getAuthoritiesByProjectRole(role: String, projectId: UUID, visibility: Boolean?): List<String>
	fun getOptionAuthoritiesByProject(projectId: UUID, projectOptions: List<ProjectOptionEnum>): List<String>
	fun getProjectIdsFromCurrentUserProfiles(currentUser: CurrentUserModel): List<UUID>
	fun getAssignableUserRoles(currentUser: CurrentUserModel): List<String>
	fun getAssignableProjectRoles(profile: ProjectProfileModel): List<String>
}
