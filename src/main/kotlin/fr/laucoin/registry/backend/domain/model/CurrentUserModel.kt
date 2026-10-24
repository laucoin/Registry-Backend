package fr.laucoin.registry.backend.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class CurrentUserModel(
	private var authorities: MutableCollection<GrantedAuthority> = mutableListOf(),
	var preferences: PreferencesModel? = null,
): UserModel(), UserDetails {
	constructor(user: UserModel): this() {
		id = user.id
		oidcId = user.oidcId
		type = user.type
		firstName = user.firstName
		lastName = user.lastName
		email = user.email
		role = user.role
		birthday = user.birthday
		lastLogin = user.lastLogin
		visible = user.visible
		creation = user.creation
		lastEdition = user.lastEdition
	}

	fun promote(newAuthorities: List<String>) = newAuthorities.forEach { authorities.add(SimpleGrantedAuthority(it)) }

	fun hasAuthority(authority: String): Boolean = authorities.any { it.authority == authority }

	/** Ids of the projects on which the user holds [permission]. */
	fun projectIdsWith(permission: String): List<UUID> = authorities
		.mapNotNull { it.authority }
		.filter { it.endsWith("_$permission") }
		.mapNotNull { runCatching { UUID.fromString(it.removeSuffix("_$permission")) }.getOrNull() }

	/** Hidden elements are reserved to the callers holding [permission] on [projectId]: the others only get visible ones. */
	fun visibilityFor(requested: Boolean?, projectId: UUID, permission: String): Boolean? =
		if (hasAuthority("${projectId}_$permission")) requested else true

	fun hasAuthority(projectId: UUID, authority: String): Boolean =
		authorities.any { it.authority == "${projectId}_$authority" }

	override fun getAuthorities(): MutableCollection<GrantedAuthority> = authorities

	@JsonIgnore
	override fun getUsername(): String = oidcId.toString()

	@JsonIgnore
	override fun getPassword(): String? = null
}
