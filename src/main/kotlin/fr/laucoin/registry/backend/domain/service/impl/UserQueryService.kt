package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.extension.StringExt.toNormalizedEmail
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserQueryService
import java.time.Clock
import java.util.UUID
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Consulte les utilisateurs et porte le compte de service chargé au démarrage.
* Périmètre : Lecture des utilisateurs (le compte de service est toujours exclu) et rôles attribuables par l'appelant.
 * Limites : Ne modifie rien ; le compte de service est chargé une seule fois, au rafraîchissement du contexte.
 */
@Service
class UserQueryService(
	private val port: IUserPort,
	private val roleService: IRoleService,
	clock: Clock,
) : ApplicationListener<ContextRefreshedEvent>, IUserQueryService, GenericService(clock) {
	private lateinit var serviceAccount: CurrentUserModel

	// Le blocage est voulu : cela s'exécute une seule fois sur le thread de rafraîchissement du contexte
	override fun onApplicationEvent(event: ContextRefreshedEvent) {
		serviceAccount = port.findServiceAccount().blockOptional()
			.orElseThrow { IllegalStateException("Service account not found") }
	}

	override fun findUsersPage(
		pageable: PageableModel,
		searchParams: UserSearchParamModel
	): Mono<PageModel<UserModel>> =
		port.findPage(pageable, searchParams)

	override fun findUserById(id: UUID, isVisible: Boolean?): Mono<UserModel> =
		port.findById(id, isVisible)
			.filter { isNotServiceAccount(it) }
			.notFoundIfEmpty(id)

	private fun isNotServiceAccount(user: UserModel): Boolean = user.id != serviceAccount.id

	override fun findUserByOidcId(id: UUID, isVisible: Boolean?): Mono<CurrentUserModel> =
		port.findByOidcId(id, isVisible)
			.filter { isNotServiceAccount(it) }

	override fun findUserByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserModel> {
		return port.findByEmail(email, isVisible)
			.filter { isNotServiceAccount(it) }
	}

	override fun findUsersByEmails(emails: Collection<String>): Flux<UserModel> {
		return port.findByEmails(emails.map { it.toNormalizedEmail() }, isVisible = null)
			.filter { isNotServiceAccount(it) }
	}

	override fun serviceAccount(): CurrentUserModel = serviceAccount

	override fun assignableUserRoles(currentUser: CurrentUserModel): Flux<String> =
		Flux.fromIterable(roleService.getAssignableUserRoles(currentUser))
}
