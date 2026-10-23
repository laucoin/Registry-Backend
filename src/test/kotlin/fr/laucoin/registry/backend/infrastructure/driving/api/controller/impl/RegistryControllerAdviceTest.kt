package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.CONCURRENT_UPDATE_CONFLICT
import fr.laucoin.registry.backend.domain.service.ITranslateService
import io.r2dbc.spi.R2dbcNonTransientResourceException
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.dao.CannotAcquireLockException
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.r2dbc.UncategorizedR2dbcException
import org.springframework.web.server.i18n.LocaleContextResolver
import org.springframework.context.i18n.SimpleLocaleContext

class RegistryControllerAdviceTest {
	private val translateService: ITranslateService = mock()
	private val localeContextResolver: LocaleContextResolver = mock()
	private val advice = RegistryControllerAdvice(translateService, localeContextResolver)
	private val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v2/users"))

	private fun givenTranslations() {
		whenever(localeContextResolver.resolveLocaleContext(any())).thenReturn(SimpleLocaleContext(Locale.ENGLISH))
		whenever(translateService.getError(any(), anyOrNull(), anyOrNull(), any())).thenReturn("translated")
	}

	@ParameterizedTest
	@ValueSource(strings = ["55P03", "40P01", "40001"])
	fun `Should handleException return a conflict when a concurrency sql state is in the causes`(sqlState: String) {
		// Arrange
		givenTranslations()
		val cause = R2dbcNonTransientResourceException("lock", sqlState)
		val exception = UncategorizedR2dbcException("query", "select", cause)

		// Act
		val result = advice.handleException(exception, exchange).block()

		// Assert
		assertEquals(HttpStatus.CONFLICT, result?.statusCode)
		assertEquals(CONCURRENT_UPDATE_CONFLICT, result?.body?.code)
	}

	@Test
	fun `Should handleException return a conflict when the exception is a concurrency failure`() {
		// Arrange
		givenTranslations()

		// Act
		val result = advice.handleException(CannotAcquireLockException("lock"), exchange).block()

		// Assert
		assertEquals(HttpStatus.CONFLICT, result?.statusCode)
		assertEquals(CONCURRENT_UPDATE_CONFLICT, result?.body?.code)
	}

	@Test
	fun `Should handleException return an internal error when the sql state is not a concurrency one`() {
		// Arrange
		givenTranslations()
		val exception = UncategorizedR2dbcException("query", "select", R2dbcNonTransientResourceException("x", "23505"))

		// Act
		val result = advice.handleException(exception, exchange).block()

		// Assert
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result?.statusCode)
		assertEquals("500", result?.body?.code)
	}
}
