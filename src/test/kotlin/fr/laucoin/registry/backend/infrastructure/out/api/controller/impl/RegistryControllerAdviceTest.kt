package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.support.DefaultMessageSourceResolvable
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.validation.FieldError
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException

/**
 * The advice is the single place that turns a refusal into the translated
 * ErrorDto, so it is also the single place where a message's variables can go
 * missing. Bean Validation prefixes its arguments with a resolvable naming the
 * field, which shifted every placeholder by one: a bundle entry written for the
 * constraint attribute rendered the field's name instead. These cases pin the
 * argument array the message source actually receives.
 */
class RegistryControllerAdviceTest {
	private val translateService: ITranslateService = mock<ITranslateService>().also {
		whenever(it.getError(any(), anyOrNull(), anyOrNull())).thenReturn("translated")
	}
	private val advice = RegistryControllerAdvice(translateService)

	private fun capturedArgs(code: String): Array<Any>? {
		val captor = argumentCaptor<Array<Any>>()
		verify(translateService).getError(eq("$ERROR_MESSAGE_PREFIX$code"), captor.capture(), anyOrNull())
		return captor.firstValue
	}

	@Test
	fun `Should forward the exception arguments of a RegistryException`() {
		// Arrange
		val exception = RegistryException(CONFLICT, PROJECT_DATE_CONFLICT_WITH_ELEMENTS, arrayListOf("Spike"))

		// Act
		advice.handleRegistryException(exception).block()

		// Assert
		assertArrayEquals(arrayOf<Any>("Spike"), capturedArgs(PROJECT_DATE_CONFLICT_WITH_ELEMENTS))
	}

	@Test
	fun `Should drop the field resolvable so a bind error starts at the constraint attribute`() {
		// Arrange
		val error = FieldError(
			"projectWriterDto",
			"name",
			null,
			false,
			arrayOf("Size.projectWriterDto.name"),
			arrayOf(DefaultMessageSourceResolvable("name"), 150, 0),
			PROJECT_NAME_TOO_LONG,
		)
		val exception = mock<WebExchangeBindException>()
		whenever(exception.allErrors).thenReturn(listOf(error))

		// Act
		advice.handleWebExchangeBindException(exception).block()

		// Assert
		assertArrayEquals(arrayOf<Any>(150, 0), capturedArgs(PROJECT_NAME_TOO_LONG))
	}

	@Test
	fun `Should drop the field resolvable so a method validation error starts at the constraint attribute`() {
		// Arrange
		val resolvable = DefaultMessageSourceResolvable(
			arrayOf("Max.pageSize"),
			arrayOf(DefaultMessageSourceResolvable("pageSize"), 200),
			PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE,
		)
		val valueResult = mock<ParameterValidationResult>()
		whenever(valueResult.resolvableErrors).thenReturn(listOf(resolvable))
		val exception = mock<HandlerMethodValidationException>()
		whenever(exception.valueResults).thenReturn(listOf(valueResult))

		// Act
		advice.handleHandlerMethodValidationException(exception).block()

		// Assert
		assertArrayEquals(arrayOf<Any>(200), capturedArgs(PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE))
	}

	@Test
	fun `Should fall back to the rejected value when the constraint carries no attribute`() {
		// Arrange
		val resolvable = DefaultMessageSourceResolvable(
			arrayOf("Sort.sort"),
			arrayOf(DefaultMessageSourceResolvable("sort")),
			PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE,
		)
		val valueResult = mock<ParameterValidationResult>()
		whenever(valueResult.resolvableErrors).thenReturn(listOf(resolvable))
		whenever(valueResult.argument).thenReturn("nope")
		val exception = mock<HandlerMethodValidationException>()
		whenever(exception.valueResults).thenReturn(listOf(valueResult))

		// Act
		advice.handleHandlerMethodValidationException(exception).block()

		// Assert
		assertArrayEquals(arrayOf<Any>("nope"), capturedArgs(PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE))
	}
}
