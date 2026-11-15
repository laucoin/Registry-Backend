package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import org.mapstruct.Named
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Component

/**
 * Objectif : Extrait les noms d'autorités de l'utilisateur courant pour l'API.
 * Périmètre : Ignore les autorités sans nom.
 * Limites : Ne modifie pas l'utilisateur.
 */
@Component
class AuthorityMappingHelper {
	@Named("authorityNames")
	fun authorityNames(model: CurrentUserModel): List<String> = model.authorities.mapNotNull(GrantedAuthority::getAuthority)
}
