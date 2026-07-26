package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Duration
import java.util.stream.Stream
import kotlin.test.assertEquals

class GenericDurationReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = object : GenericDurationReaderDtoMapper(translateService) {}

	/**
	 * Echo the resolved key plus its args so each branch's routing is
	 * observable: "code" when no args, "code[a, b]" otherwise.
	 */
	@BeforeEach
	fun setUp() {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull()))
			.thenAnswer {
				val code = it.arguments[0] as String
				val args = it.arguments[1] as Array<*>?
				if (args == null) code else "$code${args.contentToString()}"
			}
	}

	@ParameterizedTest
	@MethodSource("durations")
	fun `Should route a duration to its translation key`(duration: Duration, expected: String) {
		// Act + Assert
		assertEquals(expected, mapper.formatDuration(duration))
	}

	companion object {
		/**
		 * Sub-minute durations are fuzzy by design ("a few seconds", no args) —
		 * exact seconds would push clients into per-second re-renders. Minutes
		 * and above keep their exact numbers.
		 */
		@JvmStatic
		fun durations(): Stream<Arguments> = Stream.of(
			Arguments.of(Duration.ofSeconds(0), "duration.seconds"),
			Arguments.of(Duration.ofSeconds(1), "duration.seconds"),
			Arguments.of(Duration.ofSeconds(59), "duration.seconds"),
			Arguments.of(Duration.ofMinutes(1), "duration.minute"),
			Arguments.of(Duration.ofMinutes(30), "duration.minutes[30]"),
			Arguments.of(Duration.ofMinutes(60), "duration.minutes[60]"),
			Arguments.of(Duration.ofMinutes(90), "duration.hour"),
			Arguments.of(Duration.ofHours(5), "duration.hours[5]"),
			Arguments.of(Duration.ofDays(1), "duration.day"),
			Arguments.of(Duration.ofDays(12), "duration.days[12]"),
			Arguments.of(Duration.ofDays(45), "duration.month"),
			Arguments.of(Duration.ofDays(120), "duration.months[4]"),
			Arguments.of(Duration.ofDays(400), "duration.year"),
			Arguments.of(Duration.ofDays(800), "duration.years[2]"),
		)
	}
}
