package fr.laucoin.registry.backend.infrastructure.driving.security.service

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_BLOCKED_ACCOUNT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_ALREADY_USED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN
import fr.laucoin.registry.backend.domain.extension.UserExt.getClaimAsUUID
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.support.IRoleService
import fr.laucoin.registry.backend.domain.service.usecase.IUserUseCase
import fr.laucoin.registry.backend.domain.service.LoggerService
import java.util.UUID
import org.springframework.core.convert.converter.Converter
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.LOCKED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Objectif : Convertit un JWT validé en principal de l'utilisateur courant.
 * Périmètre : Charge ou provisionne l'utilisateur, refuse les comptes bloqués et construit ses autorités.
 * Limites : Ne conserve aucune session ; le principal est mis en cache par le cache de principals.
 */
@Component
class TokenConverterService(
	private val userUseCase: IUserUseCase,
	private val profilePort: IProjectProfilePort,
	private val roleService: IRoleService,
	private val principalCache: IPrincipalCacheService,
	private val claims: SecurityClaimsProperties,
): Converter<Jwt, Mono<AbstractAuthenticationToken>>, LoggerService() {

	override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
		if (!jwt.hasClaim(claims.userId) || !jwt.hasClaim(claims.email)) {
			log.error("The \"{}\" and \"{}\" keys are not found in the token", claims.userId, claims.email)
			return Mono.error(JwtConversionException(UNAUTHORIZED, AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN))
		}

		val token = TokenClaims(
			oidcId = jwt.getClaimAsUUID(claims.userId)!!,
			email = jwt.getClaimAsString(claims.email)!!,
			emailVerified = jwt.getClaimAsBoolean(claims.emailVerified) == true,
			firstName = jwt.getClaimAsString(claims.firstName),
			lastName = jwt.getClaimAsString(claims.lastName),
		)

		return principalCache.get(token.oidcId) { resolveCurrentUser(token) }
			.map { UsernamePasswordAuthenticationToken(it, null, it.authorities) }
	}

	// Pas de cache en cas d'échec : l'AsyncCache de Caffeine supprime une entrée dont le future se termine
	// en exception, donc un utilisateur bloqué échoue toujours à neuf depuis la base de données.
	private fun resolveCurrentUser(token: TokenClaims): Mono<CurrentUserModel> =
		userUseCase.findUserByOidcId(token.oidcId, isVisible = null)
			.throwOnBlockedUser()
			.updateUserIfPersonalDataChanged(token)
			.createNewUserOnNotFound(token)
			.buildAuthorities()

	private fun Mono<CurrentUserModel>.throwOnBlockedUser(): Mono<CurrentUserModel> = handle { user, sink -> sink.nextOrError(user, blockedAccountError(user)) }

	private fun blockedAccountError(user: CurrentUserModel): JwtConversionException? {
		if (!user.isNotVisible()) return null
		log.warn("Signing in attempt blocked for user \"{}\" due to disabled account", user.id)
		return JwtConversionException(LOCKED, AUTH_BLOCKED_ACCOUNT)
	}

	private fun Mono<CurrentUserModel>.updateUserIfPersonalDataChanged(token: TokenClaims): Mono<CurrentUserModel> =
		flatMap { updatePersonalData(it, token) }

	private fun updatePersonalData(user: CurrentUserModel, token: TokenClaims): Mono<CurrentUserModel> {
		return userUseCase.updateUserIfPersonalDataChanged(user, token.email, token.firstName, token.lastName)
			.map { user }
	}

	private fun Mono<CurrentUserModel>.createNewUserOnNotFound(token: TokenClaims): Mono<CurrentUserModel> =
		switchIfEmpty { provisionUnknownUser(token) }

	private fun provisionUnknownUser(token: TokenClaims): Mono<CurrentUserModel> {
		log.info("User with OIDC ID \"{}\" not found, checking whether an account exists with the same email", token.oidcId)
		return userUseCase.findUserByEmail(token.email, isVisible = null)
			.collectList()
			.flatMap { provisionFromEmailMatches(it, token) }
			.onErrorMap(DataIntegrityViolationException::class.java) { emailAlreadyUsed(token) }
	}

	private fun provisionFromEmailMatches(matches: List<CurrentUserModel>, token: TokenClaims): Mono<CurrentUserModel> {
		return when {
			matches.isEmpty() -> {
				log.info("No user found with the given email, creating a new user with OIDC ID \"{}\"", token.oidcId)
				userUseCase.createUser(token.oidcId, token.email, token.firstName, token.lastName)
			}

			matches.size == 1 && token.emailVerified && matches.first().oidcId == null -> {
				log.info("Linking existing user with the given email to OIDC ID \"{}\"", token.oidcId)
				userUseCase.linkUserToOidcId(matches.first(), token.oidcId)
			}

			else -> {
				log.warn("Cannot link OIDC ID \"{}\": ambiguous, already linked or unverified email", token.oidcId)
				Mono.error(emailAlreadyUsed(token))
			}
		}
	}

	private fun emailAlreadyUsed(token: TokenClaims) =
		JwtConversionException(CONFLICT, AUTH_EMAIL_ALREADY_USED, arrayListOf(token.email))

	private fun Mono<CurrentUserModel>.buildAuthorities(): Mono<CurrentUserModel> = flatMap(::withAuthorities)

	private fun withAuthorities(user: CurrentUserModel): Mono<CurrentUserModel> {
		user.promote(roleService.getAuthoritiesByUserRole(user.role))
		return profilePort.findProjectProfilesRolesByUserId(user.id!!)
			.collectList()
			.map { profiles -> user.apply { profiles.forEach { promoteProjectAuthorities(it) } } }
	}

	private fun CurrentUserModel.promoteProjectAuthorities(profile: ProjectProfileRoleModel) {
		promote(roleService.getAuthoritiesByProjectRole(profile.role!!, profile.projectId!!, profile.projectVisible))
		if (profile.projectVisible != true) return
		promote(roleService.getOptionAuthoritiesByProject(profile.projectId!!, profile.projectOptions ?: emptyList()))
	}

	private data class TokenClaims(
		val oidcId: UUID,
		val email: String,
		val emailVerified: Boolean,
		val firstName: String?,
		val lastName: String?,
	)
}
