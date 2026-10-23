package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.config.SecurityConfig.Companion.apiOrigin
import fr.laucoin.registry.backend.config.SecurityConfig.Companion.documentationContentSecurityPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SecurityConfigDocumentationPolicyTest {
	@Test
	fun `Should apiOrigin build the origin of the API on the requested host`() {
		// Act
		val result = apiOrigin("https", "docs.example.org", 8081)

		// Assert
		assertEquals("https://docs.example.org:8081", result)
	}

	@Test
	fun `Should apiOrigin refuse a host name able to alter the policy`() {
		// Act
		val result = apiOrigin("https", "evil.org; script-src *", 8081)

		// Assert
		assertNull(result)
	}

	@Test
	fun `Should documentationContentSecurityPolicy restrict connections to known origins`() {
		// Act
		val result = documentationContentSecurityPolicy("https://idp.example.org", "https://docs.example.org:8081")

		// Assert
		assertEquals(
			"default-src 'self'; connect-src 'self' https://idp.example.org https://docs.example.org:8081; frame-ancestors 'none'",
			result,
		)
	}

	@Test
	fun `Should documentationContentSecurityPolicy omit the API origin when the host is unknown`() {
		// Act
		val result = documentationContentSecurityPolicy("https://idp.example.org", null)

		// Assert
		assertEquals("default-src 'self'; connect-src 'self' https://idp.example.org; frame-ancestors 'none'", result)
	}
}
