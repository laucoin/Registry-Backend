package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import java.util.UUID

/**
 * Resolves the in-memory RBAC model loaded at startup: role levels, the authorities a User- or
 * Project-level role grants (including option-gated authorities), and which roles the caller is
 * allowed to assign to someone else. Role/permission data itself is seeded by migrations, not by
 * this service — a change needs both a migration and a restart (ADR 005).
 */
interface IRoleService {
	fun getLevelByUserRole(role: String?): Int?
	fun getLevelByProjectRole(role: String): Int
	fun getLevel0RoleFromProjectRoles(): String
	fun getDefaultUserRole(): String?
	fun getAuthoritiesByUserRole(role: String?): List<String>
	fun getAuthoritiesByProjectRole(role: String, projectId: UUID, visibility: Boolean?): List<String>
	fun getOptionAuthoritiesByProject(projectId: UUID, projectOptions: List<ProjectOptionEnum>): List<String>
	fun getProjectIdsFromCurrentUserProfiles(currentUser: CurrentUserModel): List<UUID>
	fun getAssignableUserRoles(currentUser: CurrentUserModel): List<String>
	fun getAssignableProjectRoles(profile: ProjectProfileModel): List<String>
}
