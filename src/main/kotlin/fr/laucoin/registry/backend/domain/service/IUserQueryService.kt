package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de lecture des utilisateurs.
* Périmètre : Déclare la consultation (page, identifiant, OIDC, e-mail), le compte de service et les rôles
 *   attribuables.
 * Limites : Contrat uniquement ; ne modifie aucun utilisateur.
 */
interface IUserQueryService {
	fun findUsersPage(pageable: PageableModel, searchParams: UserSearchParamModel): Mono<PageModel<UserModel>>

	fun findUserById(id: UUID, isVisible: Boolean?): Mono<UserModel>

	fun findUserByOidcId(id: UUID, isVisible: Boolean?): Mono<CurrentUserModel>

	fun findUserByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserModel>

	fun findUsersByEmails(emails: Collection<String>): Flux<UserModel>

	fun assignableUserRoles(currentUser: CurrentUserModel): Flux<String>

	fun serviceAccount(): CurrentUserModel
}
