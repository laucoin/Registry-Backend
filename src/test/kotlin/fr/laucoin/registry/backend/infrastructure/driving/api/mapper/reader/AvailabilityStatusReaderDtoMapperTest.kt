package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import java.time.Clock
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum.PROFILE
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AvailabilityStatusReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper = AvailabilityStatusReaderDtoMapper(translateService, Clock.systemUTC())

	private companion object {
		private const val NOT_YET = "availability.status.profile.duration.NOT_YET"
		private const val NO_MORE = "availability.status.profile.duration.NO_MORE"
		private const val AVAILABLE_SINCE = "availability.status.profile.duration.AVAILABLE"
		private const val UNAVAILABLE_PLAIN = "availability.status.profile.UNAVAILABLE"
	}

	init {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any()))
			.thenAnswer { "${it.getArgument<String>(0)}|${it.getArgument<Array<Any>?>(1)?.joinToString()}" }
	}

	private fun day(offset: Long) = CustomDateTimeModel(LocalDate.now(ZoneOffset.UTC).plusDays(offset))

	private fun label(status: AvailabilityStatusEnum, start: CustomDateTimeModel?, end: CustomDateTimeModel?): String =
		mapper.toDto(status, PROFILE, start, end).label

	@Test
	fun `should announce the time before the start when the start is in the future`() {
		// Arrange
		val start = day(5)
		val end = day(10)

		// Act
		val label = label(UNAVAILABLE, start, end)

		// Assert
		assertTrue(label.startsWith("$NOT_YET|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `should announce the time since the end when the end is in the past`() {
		// Arrange
		val start = day(-10)
		val end = day(-5)

		// Act
		val label = label(UNAVAILABLE, start, end)

		// Assert
		assertTrue(label.startsWith("$NO_MORE|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `should announce the time before the start when there is no end`() {
		// Arrange
		val start = day(3)

		// Act
		val label = label(UNAVAILABLE, start, null)

		// Assert
		assertTrue(label.startsWith("$NOT_YET|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `should announce the time since the end when there is no start`() {
		// Arrange
		val end = day(-3)

		// Act
		val label = label(UNAVAILABLE, null, end)

		// Assert
		assertTrue(label.startsWith("$NO_MORE|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `should announce the time since the start when the element is available`() {
		// Arrange
		val start = day(-4)
		val end = day(4)

		// Act
		val label = label(AVAILABLE, start, end)

		// Assert
		assertTrue(label.startsWith("$AVAILABLE_SINCE|"), label)
		assertFalse(label.contains("-"), label)
	}

	@Test
	fun `should fall back on the plain status when there is no date`() {
		// Arrange
		val start: CustomDateTimeModel? = null

		// Act
		val label = label(UNAVAILABLE, start, null)

		// Assert
		assertEquals("$UNAVAILABLE_PLAIN|null", label)
	}

	@Test
	fun `should word the label with the translations of the subject`() {
		// Arrange
		val start = day(5)

		// Act
		val labels = AvailabilitySubjectEnum.entries
			.associateWith { mapper.toDto(UNAVAILABLE, it, start, null).label }

		// Assert
		AvailabilitySubjectEnum.entries.forEach {
			assertTrue(labels.getValue(it).startsWith("availability.status.${it.name.lowercase()}.duration.NOT_YET|"))
		}
	}

	@Test
	fun `should keep the generic translations for the metadata labels`() {
		// Arrange
		val status = AVAILABLE

		// Act
		val label = mapper.toDto(status).label

		// Assert
		assertEquals("availability.status.AVAILABLE|null", label)
	}
}
