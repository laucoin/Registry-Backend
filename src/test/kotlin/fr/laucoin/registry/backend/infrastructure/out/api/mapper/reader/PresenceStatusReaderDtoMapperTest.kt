package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals

class PresenceStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = PresenceStatusReaderDtoMapper(translateService)

	private companion object {
		@JvmStatic
		fun `Should not describe a window that has neither opened late nor closed early`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(
					CustomDateTimeModel(LocalDate.now().minusDays(2)),
					CustomDateTimeModel(LocalDate.now().plusDays(2)),
				),
				Arguments.of(null, CustomDateTimeModel(LocalDate.now())),
				Arguments.of(CustomDateTimeModel(LocalDate.now()), null),
			)
	}

	@BeforeEach
	fun setUp() {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull()))
			.thenAnswer { it.arguments[0] }
	}

	/**
	 * Regression: a null availability window used to route into
	 * Duration.between(now, null) and crash the response for every entity
	 * carrying it — e.g. a freshly created participant with no availability.
	 */
	@Test
	fun `Should fall back to the plain status label when no movement or window is present`() {
		// Act
		val result = mapper.toDto(UNAVAILABLE, lastMovement = null, startAvailability = null, endAvailability = null)

		// Assert
		assertEquals(UNAVAILABLE.name, result.value)
		assertEquals("${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET", result.label)
	}

	@Test
	fun `Should compute the elapsed duration since the last movement`() {
		// Act
		val result = mapper.toDto(
			IN,
			lastMovement = ZonedDateTime.now().minusHours(2),
			startAvailability = null,
			endAvailability = null,
		)

		// Assert
		assertEquals("$PRESENCE_STATUS_DURATION_PREFIX$IN", result.label)
	}

	/**
	 * The direction question the v1 mapper left open, now settled: ARRIVE
	 * ("arrives in {0}") only while the window has yet to open, LEFT ("left {0}
	 * ago") only once it has closed. The old conditions were the mirror image,
	 * so someone expected in two days was announced as having already left.
	 */
	@Test
	fun `Should announce the arrival countdown only before the window opens`() {
		// Act
		val result = mapper.toDto(
			UNAVAILABLE,
			lastMovement = null,
			startAvailability = CustomDateTimeModel(LocalDate.now().plusDays(2)),
			endAvailability = null,
		)

		// Assert
		assertEquals("${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE", result.label)
	}

	@Test
	fun `Should announce the left label only after the window closes`() {
		// Act
		val result = mapper.toDto(
			UNAVAILABLE,
			lastMovement = null,
			startAvailability = null,
			endAvailability = CustomDateTimeModel(LocalDate.now().minusDays(2)),
		)

		// Assert
		assertEquals("${PRESENCE_STATUS_DURATION_PREFIX}LEFT", result.label)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should not describe a window that has neither opened late nor closed early`(
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	) {
		// Act
		val result = mapper.toDto(UNAVAILABLE, lastMovement = null, startAvailability, endAvailability)

		// Assert
		assertEquals("${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET", result.label)
	}
}
