package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.test.ModelExt.activityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.time.Duration.Companion.minutes

/**
 * An outing carries its activity's planned duration so a caller can tell an
 * overrun from a normal outing without asking for the activity: the dashboard
 * used to fetch every listed activity one by one to draw that warning.
 */
class MovementActivityReasonReaderDtoMapperTest {
	private val translateService: ITranslateService = mock<ITranslateService>().also {
		whenever(it.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("activity")
	}
	private val mapper = MovementActivityReasonReaderDtoMapper(translateService)

	private companion object {
		private fun activity(duration: kotlin.time.Duration?) = ActivityModel().apply {
			name = "Rando"
			this.duration = duration
			id = activityId
		}

		@JvmStatic
		fun `Should carry the activity's planned duration as an ISO-8601 string`(): Stream<Arguments> = Stream.of(
			Arguments.of(120L, "PT2H"),
			Arguments.of(90L, "PT1H30M"),
			Arguments.of(45L, "PT45M"),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should carry the activity's planned duration as an ISO-8601 string`(
		plannedMinutes: Long,
		expected: String,
	) {
		// Act
		val dto = mapper.toDto(activity(plannedMinutes.minutes))

		// Assert
		assertEquals(expected, dto.duration)
	}

	/**
	 * An activity that states no duration cannot overrun, and says so by sending
	 * nothing rather than a zero a caller would have to interpret.
	 */
	@Test
	fun `Should carry no duration when the activity states none`() {
		// Act
		val dto = mapper.toDto(activity(duration = null))

		// Assert
		assertNull(dto.duration)
	}

	@Test
	fun `Should name the activity and keep it identified as one`() {
		// Act
		val dto = mapper.toDto(activity(60.minutes))

		// Assert
		assertEquals("Rando\u00A0(activity)", dto.label)
		assertEquals(activityId.toString(), dto.value)
		assertEquals(ACTIVITY, dto.kind)
	}
}
