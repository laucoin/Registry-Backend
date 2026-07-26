package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import java.util.stream.Stream

class TranslateServiceTest {
	private val messagesSource: MessageSource = mock()
	private val errorsSource: MessageSource = mock()
	private val service: ITranslateService = TranslateService(messagesSource, errorsSource)

	private companion object {
		@JvmStatic
		fun translationData(): Stream<Arguments> = Stream.of(
			Arguments.of("code", arrayOf("arg1", "arg2"), "default message", "default message"),
			Arguments.of("code", arrayOf("arg1", "arg2"), null, "code"),
			Arguments.of("code", null, null, "code"),
		)
	}

	@ParameterizedTest
	@MethodSource("translationData")
	fun `Should getMessage return the translation`(
		code: String,
		args: Array<Any>?,
		default: String?,
		expected: String,
	) {
		// Arrange
		whenever(messagesSource.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenReturn("translated")

		// Act
		val result = service.getMessage(code, args, default)

		// Assert
		assertEquals("translated", result)

		verify(messagesSource).getMessage(code, args, expected, LocaleContextHolder.getLocale())
	}

	@ParameterizedTest
	@MethodSource("translationData")
	fun `Should getError return the translation`(
		code: String,
		args: Array<Any>?,
		default: String?,
		expected: String,
	) {
		// Arrange
		whenever(errorsSource.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenReturn("translated")

		// Act
		val result = service.getError(code, args, default)

		// Assert
		assertEquals("translated", result)

		verify(errorsSource).getMessage(code, args, expected, LocaleContextHolder.getLocale())
	}
}
