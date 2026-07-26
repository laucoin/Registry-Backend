package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class PresenceStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = PresenceStatusReaderDtoMapper(translateService)

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
	 * The previously-NPEing branch, now exercised with a non-null bound.
	 * (Direction semantics are frozen v1 behaviour — asStartIsAfterOther
	 * fires ARRIVE for a past-dated start; we pin the behaviour, not judge it.)
	 */
	@Test
	fun `Should render the arrival-window branch with a real bound (no crash)`() {
		// Act
		val result = mapper.toDto(
			UNAVAILABLE,
			lastMovement = null,
			startAvailability = CustomDateTimeModel(LocalDate.now().minusDays(2)),
			endAvailability = null,
		)

		// Assert
		assertEquals("${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE", result.label)
	}
}
