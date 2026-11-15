package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocaleFluxExt.mapInRequestLocale
import java.util.Locale
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.i18n.LocaleContextThreadLocalAccessor
import org.springframework.context.i18n.SimpleLocaleContext
import reactor.core.publisher.Flux
import reactor.util.context.Context

class LocaleFluxExtTest {
	@AfterEach
	fun resetLocale() {
		LocaleContextHolder.resetLocaleContext()
	}

	@Test
	fun `Should mapInRequestLocale map each element in the locale of the request, not in the locale of the thread`() {
		// Arrange
		LocaleContextHolder.setLocale(Locale.ENGLISH)
		val request = Context.of(LocaleContextThreadLocalAccessor.KEY, SimpleLocaleContext(Locale.FRENCH))

		// Act
		val languages = Flux.just(1, 2)
			.mapInRequestLocale { LocaleContextHolder.getLocale().language }
			.contextWrite(request)
			.collectList()
			.block()

		// Assert
		assertEquals(listOf("fr", "fr"), languages)
	}
}
