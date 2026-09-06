package fr.laucoin.registry.backend.domain.model

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * The one-time values that tie an authorization request to the browser that started it.
 *
 * `state` is OAuth's CSRF token. Without it, an attacker holding a valid authorization code for
 * their own identity can navigate a victim's browser to the callback and silently sign that victim
 * into the attacker's account — everything the victim then records lands in the attacker's data.
 *
 * The PKCE verifier answers a different question: it proves that whoever redeems the code is the
 * same party that asked for it. The client secret does not do that — it authenticates the
 * application, not the request — which is why the two are used together rather than one instead of
 * the other.
 */
data class AuthorizationChallengeModel(
	val state: String,
	val nonce: String,
	val codeVerifier: String,
) {
	/** The S256 transformation of the verifier: what travels in the URL, in the clear. */
	val codeChallenge: String
		get() = URL_ENCODER.encodeToString(
			MessageDigest.getInstance(DIGEST).digest(codeVerifier.toByteArray(Charsets.US_ASCII))
		)

	companion object {
		const val CHALLENGE_METHOD = "S256"

		private const val DIGEST = "SHA-256"
		private const val ENTROPY_BYTES = 32

		private val URL_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
		private val RANDOM = SecureRandom()

		fun generate() = AuthorizationChallengeModel(
			state = randomValue(),
			nonce = randomValue(),
			codeVerifier = randomValue(),
		)

		/**
		 * 32 bytes from [SecureRandom], base64url without padding — 43 characters, within the 43–128
		 * range RFC 7636 requires of a verifier, and unguessable enough to serve as `state` too.
		 */
		private fun randomValue(): String {
			val bytes = ByteArray(ENTROPY_BYTES)
			RANDOM.nextBytes(bytes)
			return URL_ENCODER.encodeToString(bytes)
		}
	}
}
