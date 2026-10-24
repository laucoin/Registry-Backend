package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.Duration
import java.util.Locale
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.support.ReloadableResourceBundleMessageSource

class GenericDurationReaderDtoMapperTest {
	private class DurationMapper(translateService: ITranslateService) : GenericDurationReaderDtoMapper(translateService)

	private val messageSource = ReloadableResourceBundleMessageSource().apply {
		setBasename("classpath:i18n/messages")
		setDefaultEncoding(Charsets.UTF_8.name())
		setFallbackToSystemLocale(false)
	}

	private fun mapperFor(locale: Locale): DurationMapper {
		val translateService: ITranslateService = mock()
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenAnswer {
			messageSource.getMessage(it.getArgument<String>(0), it.getArgument<Array<Any>?>(1), locale)
		}
		return DurationMapper(translateService)
	}

	private companion object {
		private fun minutes(value: Long) = Duration.ofMinutes(value)
		private fun hours(value: Long, minutes: Long = 0) = Duration.ofHours(value).plusMinutes(minutes)
		private fun days(value: Long, hours: Long = 0) = Duration.ofDays(value).plusHours(hours)

		@JvmStatic
		fun frenchDurations(): Stream<Arguments> = Stream.of(
			Arguments.of(Duration.ZERO, "moins d’une heure"),
			Arguments.of(minutes(59), "moins d’une heure"),
			Arguments.of(hours(1), "une heure"),
			Arguments.of(hours(1, 30), "une heure et 30 minutes"),
			Arguments.of(hours(5), "5 heures"),
			Arguments.of(hours(23, 59), "23 heures et 59 minutes"),
			Arguments.of(days(1), "un jour"),
			Arguments.of(days(1, 3), "un jour et 3 heures"),
			Arguments.of(days(6, 23), "6 jours et 23 heures"),
			Arguments.of(days(7), "une semaine"),
			Arguments.of(days(9), "une semaine et 2 jours"),
			Arguments.of(days(14), "2 semaines"),
			Arguments.of(days(29), "4 semaines et un jour"),
			Arguments.of(days(30), "un mois"),
			Arguments.of(days(37), "un mois et une semaine"),
			Arguments.of(days(59), "un mois et 4 semaines"),
			Arguments.of(days(60), "2 mois"),
			Arguments.of(days(359), "11 mois et 4 semaines"),
			Arguments.of(days(364), "12 mois"),
			Arguments.of(days(365), "un an"),
			Arguments.of(days(400), "un an et un mois"),
			Arguments.of(days(694), "un an et 10 mois"),
			Arguments.of(days(725), "2 ans"),
			Arguments.of(days(730), "2 ans"),
			Arguments.of(days(800), "2 ans et 2 mois"),
		)
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("frenchDurations")
	fun `should format a duration with its two largest units in French`(duration: Duration, expected: String) {
		// Arrange
		val mapper = mapperFor(Locale.FRENCH)

		// Act
		val formatted = mapper.formatDuration(duration)

		// Assert
		assertEquals(expected, formatted)
	}

	@Test
	fun `should format a duration with its two largest units in English`() {
		// Arrange
		val mapper = mapperFor(Locale.ENGLISH)

		// Act
		val formatted = listOf(minutes(10), hours(2, 5), days(9), days(37), days(400)).map(mapper::formatDuration)

		// Assert
		assertEquals(
			listOf("less than an hour", "2 hours and 5 minutes", "a week and 2 days", "a month and a week", "a year and a month"),
			formatted,
		)
	}
}
