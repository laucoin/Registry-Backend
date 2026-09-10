package fr.laucoin.registry.backend.domain.model

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class AuthorizationChallengeModel(
	val state: String,
	val nonce: String,
	val codeVerifier: String,
) {
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

		private fun randomValue(): String {
			val bytes = ByteArray(ENTROPY_BYTES)
			RANDOM.nextBytes(bytes)
			return URL_ENCODER.encodeToString(bytes)
		}
	}
}
