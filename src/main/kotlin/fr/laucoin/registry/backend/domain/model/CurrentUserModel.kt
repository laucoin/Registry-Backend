package fr.laucoin.registry.backend.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class CurrentUserModel(
	private var authorities: MutableCollection<GrantedAuthority> = mutableListOf(),
	var preferences: PreferencesModel? = null,
) : UserModel(), UserDetails {
	constructor(user: UserModel) : this() {
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

	fun hasAuthority(projectId: UUID, authority: String): Boolean =
		authorities.any { it.authority == "${projectId}_$authority" }

	override fun getAuthorities(): MutableCollection<GrantedAuthority> = authorities

	@JsonIgnore
	override fun getUsername(): String = oidcId.toString()

	@JsonIgnore
	override fun getPassword(): String? = null
}
