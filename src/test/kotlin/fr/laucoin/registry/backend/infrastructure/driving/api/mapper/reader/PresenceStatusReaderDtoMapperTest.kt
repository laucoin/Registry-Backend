package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OVERDUE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.Clock
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PresenceStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = PresenceStatusReaderDtoMapper(translateService, Clock.systemUTC())

	@Test
	fun `Should toDto label an overdue status without any duration`() {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenAnswer { it.arguments[0] }
		val lastMovement = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)
		val startAvailability = CustomDateTimeModel(LocalDate.now(ZoneOffset.UTC).minusDays(5))
		val endAvailability = CustomDateTimeModel(LocalDate.now(ZoneOffset.UTC).minusDays(1))

		// Act
		val result = mapper.toDto(OVERDUE, lastMovement, startAvailability, endAvailability)

		// Assert
		assertEquals("OVERDUE", result.value)
		assertEquals("presence.status.OVERDUE", result.label)
	}
}
