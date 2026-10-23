package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.domain.service.IAuthenticationService
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.convert.ApplicationConversionService

class AuthenticationServicePropertyBindingTest {
	private val port: IAuthenticationPort = mock<IAuthenticationPort>().also {
		whenever(it.getLoginUri(any())).thenReturn(AuthenticationUriModel("uri"))
	}

	private fun runner(loginUris: String, logoutUris: String) = ApplicationContextRunner()
		.withInitializer { it.beanFactory.conversionService = ApplicationConversionService.getSharedInstance() }
		.withBean(IAuthenticationPort::class.java, { port })
		.withBean(AuthenticationService::class.java)
		.withPropertyValues(
			"external.idp.allowed-redirect-uris=$loginUris",
			"external.idp.allowed-logout-redirect-uris=$logoutUris",
		)

	@Test
	fun `Should bind a comma separated value into trimmed entries`() {
		// Arrange
		val runner = runner("https://a.test/cb,  https://b.test/cb ", "https://a.test")

		// Act
		runner.run { context ->
			val service = context.getBean(IAuthenticationService::class.java)

			// Assert
			assertEquals("uri", service.getLoginUri("https://a.test/cb").uri)
			assertEquals("uri", service.getLoginUri("https://b.test/cb").uri)
		}
	}

	@Test
	fun `Should bind a single value into one entry`() {
		// Arrange
		val runner = runner("https://a.test/cb", "https://a.test")

		// Act
		runner.run { context ->
			val service = context.getBean(IAuthenticationService::class.java)

			// Assert
			assertEquals("uri", service.getLoginUri("https://a.test/cb").uri)
			assertThrows(RegistryException::class.java) { service.getLoginUri("https://b.test/cb") }
		}
	}

	@ParameterizedTest
	@ValueSource(strings = ["", " ", ",", " , "])
	fun `Should fail to start when the logout list holds no entry`(logoutUris: String) {
		// Arrange
		val runner = runner("https://a.test/cb", logoutUris)

		// Act
		runner.run { context ->
			// Assert
			assertEquals("external.idp.allowed-logout-redirect-uris must not be empty", rootCause(context.startupFailure).message)
		}
	}

	@Test
	fun `Should fail to start when the login list holds no entry`() {
		// Arrange
		val runner = runner("", "https://a.test")

		// Act
		runner.run { context ->
			// Assert
			assertEquals("external.idp.allowed-redirect-uris must not be empty", rootCause(context.startupFailure).message)
		}
	}

	@Test
	fun `Should ignore the trailing comma of a list`() {
		// Arrange
		val runner = runner("https://a.test/cb,", "https://a.test")

		// Act
		runner.run { context ->
			// Assert
			assertEquals("uri", context.getBean(IAuthenticationService::class.java).getLoginUri("https://a.test/cb").uri)
		}
	}

	private fun rootCause(error: Throwable?): Throwable = generateSequence(error) { it.cause }.last()
}
