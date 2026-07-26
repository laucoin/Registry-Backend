package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_BLOCKED_ACCOUNT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_ALREADY_USED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_NOT_VERIFIED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN
import fr.laucoin.registry.backend.domain.extension.UserExt.getClaimAsBooleanOrFalse
import fr.laucoin.registry.backend.domain.extension.UserExt.getClaimAsUUID
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.LOCKED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.UUID


@Component
class TokenConverterService(
	private val userService: IUserService,
	private val profilePort: IProjectProfilePort,
	private val roleService: IRoleService,
	@param:Value($$"${registry.security.oauth2.claims.user-id}")
	private val userIdKey: String,
	@param:Value($$"${registry.security.oauth2.claims.email}")
	private val emailKey: String,
	@param:Value($$"${registry.security.oauth2.claims.email-verified}")
	private val emailVerifiedKey: String,
	@param:Value($$"${registry.security.oauth2.claims.first-name}")
	private val firstNameKey: String,
	@param:Value($$"${registry.security.oauth2.claims.last-name}")
	private val lastNameKey: String,
) : Converter<Jwt, Mono<AbstractAuthenticationToken>>, LoggerService() {

	override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
		if (!jwt.hasClaim(userIdKey) || !jwt.hasClaim(emailKey)) {
			log.error("The \"{}\" and \"{}\" keys are not found in the token", userIdKey, emailKey)
			return Mono.error(JwtConversionException(UNAUTHORIZED, AUTH_EMAIL_OR_ID_NOT_FOUND_IN_TOKEN))
		}

		val oidcId: UUID = jwt.getClaimAsUUID(userIdKey)!!
		val email: String = jwt.getClaimAsString(emailKey)!!
		val emailVerified: Boolean = jwt.getClaimAsBooleanOrFalse(emailVerifiedKey)
		val firstName: String? = jwt.getClaimAsString(firstNameKey)
		val lastName: String? = jwt.getClaimAsString(lastNameKey)

		return Mono.justOrEmpty(jwt)
			.fetchUser(oidcId)
			.throwOnBlockedUser()
			.updateUserIfPersonalDataChanged(email, firstName, lastName)
			.switchIfEmpty { resolveByEmailOrCreate(oidcId, email, emailVerified, firstName, lastName) }
			.buildAuthorities()
			.map { UsernamePasswordAuthenticationToken(it, null, it.authorities) }
	}

	private fun Mono<Jwt>.fetchUser(oidcId: UUID): Mono<CurrentUserModel> =
		flatMap {
			userService.findUserByOidcId(oidcId, visibilitySearched = null)
		}

	private fun Mono<CurrentUserModel>.throwOnBlockedUser(): Mono<CurrentUserModel> = handle { it, handle ->
		if (it.isNotVisible()) {
			log.warn("Signing in attempt blocked for user \"{}\" due to disabled account", it.id)
			handle.error(JwtConversionException(LOCKED, AUTH_BLOCKED_ACCOUNT))
		} else handle.next(it)
	}

	private fun Mono<CurrentUserModel>.updateUserIfPersonalDataChanged(
		email: String, firstName: String?, lastName: String?
	): Mono<CurrentUserModel> = flatMap { user ->
		userService.updateUserIfPersonalDataChanged(user, email, firstName, lastName)
			.map { user }
	}

	/**
	 * Runs only when the token's `sub` matches no existing `oidc_id`. Resolves the
	 * account by email:
	 * - no match → create a brand-new user (self-registration);
	 * - one match with a null `oidc_id` → an invited (email-only) account: link the
	 *   `oidc_id`, then re-apply the blocked guard (link-then-reject);
	 * - one match already bound to an `oidc_id` → the email belongs to another
	 *   identity → CONFLICT;
	 * - more than one match → defensive CONFLICT (the email unique index prevents it).
	 *
	 * Linking grants whatever the invitation already carries — its application role
	 * and its pending project profiles — so the email is identity-grade evidence
	 * here, not just a lookup key. It is therefore only accepted when the IdP
	 * asserts the address as verified; the check is fail-closed, an absent or
	 * unreadable claim counts as unverified. Only the link branch is gated:
	 * self-registration creates an unprivileged account, and the email unique index
	 * already stops it from taking over an address someone else holds.
	 *
	 * `tb_user_index_email` is global while [IUserService.findUserByEmail] hides
	 * service accounts, so a row this lookup cannot see may still own the address.
	 * The duplicate key is mapped to the same CONFLICT rather than surfacing as a
	 * 500 on every request the caller makes.
	 */
	private fun resolveByEmailOrCreate(
		oidcId: UUID, email: String, emailVerified: Boolean, firstName: String?, lastName: String?
	): Mono<CurrentUserModel> {
		log.info("User with OIDC ID \"{}\" not found, checking if an account exists with the same email", oidcId)
		return userService.findUserByEmail(email, visibilitySearched = null)
			.collectList()
			.flatMap { matches ->
				when {
					matches.isEmpty() -> {
						log.info("No account holds the address, creating a new user with OIDC ID \"{}\"", oidcId)
						userService.createUser(oidcId, email, firstName, lastName)
					}

					matches.size == 1 && matches.first().oidcId == null && !emailVerified -> {
						log.warn(
							"Invited account found, but claim \"{}\" does not assert the address as verified, "
								+ "refusing to link OIDC ID \"{}\"",
							emailVerifiedKey,
							oidcId
						)
						Mono.error(JwtConversionException(FORBIDDEN, AUTH_EMAIL_NOT_VERIFIED, arrayListOf(email)))
					}

					matches.size == 1 && matches.first().oidcId == null -> {
						log.info("Invited account found, linking OIDC ID \"{}\"", oidcId)
						Mono.just(matches.first())
							.flatMap { userService.linkUser(it, oidcId, email, firstName, lastName) }
							.throwOnBlockedUser()
					}

					else -> {
						log.warn("The address is already used, cannot link OIDC ID \"{}\"", oidcId)
						Mono.error(JwtConversionException(CONFLICT, AUTH_EMAIL_ALREADY_USED))
					}
				}
			}
			.onErrorMap(DuplicateKeyException::class.java) {
				log.warn(
					"The address is held by an account excluded from the lookup, cannot resolve OIDC ID \"{}\"",
					oidcId
				)
				JwtConversionException(CONFLICT, AUTH_EMAIL_ALREADY_USED)
			}
	}

	private fun Mono<CurrentUserModel>.buildAuthorities(): Mono<CurrentUserModel> = flatMap {
		it.promote(roleService.getAuthoritiesByUserRole(it.role))
		profilePort.findProjectProfilesRolesByUserId(it.id!!)
			.collectList()
			.map { profiles ->
				profiles.forEach { profile ->
					it.promote(
						roleService.getAuthoritiesByProjectRole(
							profile.role!!,
							profile.projectId!!,
							profile.projectVisible
						)
					)
					if (profile.projectVisible == true) {
						it.promote(
							roleService.getOptionAuthoritiesByProject(
								profile.projectId!!,
								projectOptions = profile.projectOptions ?: emptyList()
							)
						)
					}
				}
				it
			}
	}
}
