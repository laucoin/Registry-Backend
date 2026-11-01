package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import java.time.ZonedDateTime
import java.time.ZoneOffset
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.VEHICLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProjectReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val availabilityMapper: AvailabilityStatusReaderDtoMapper = mock()
	private val mapper: ProjectReaderDtoMapper = ProjectReaderDtoMapper(translateService, availabilityMapper)

	private companion object {
		@JvmStatic
		fun `Should toDto convert ProjectModel to ProjectReaderDto`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					ProjectModel().apply {
						id = UUID.randomUUID()
						name = "Name"
						begin = CustomDateTimeModel.MIN
						end = CustomDateTimeModel.MAX
						options = listOf(VEHICLE)
						counts = ProjectCountsModel(participants = 4, groups = 3, acceptedProfiles = 2, vehicles = 1)
						visible = true
						creation = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
						lastEdition = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
					},
					1,
				),
				Arguments.of(
					ProjectModel().apply {
						id = UUID.randomUUID()
						name = "Name"
						begin = CustomDateTimeModel.MIN
						end = CustomDateTimeModel.MAX
						options = null
						visible = true
						creation = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
						lastEdition = HistoryModel(ZonedDateTime.now(ZoneOffset.UTC))
					},
					0,
				),
			)
		}
	}

	@Test
	fun `Should toDto leave the counts empty when the project has none`() {
		// Arrange
		val project = ProjectModel().apply { counts = null }

		// Act
		val result = mapper.toDto(project)

		// Assert
		assertNull(result.counts)
	}

	@Test
	fun `Should toDto convert every count when all options are enabled`() {
		// Arrange
		val counts = ProjectCountsModel(1, 2, 3, vehicles = 4, activities = 5, activeAlerts = 6)

		// Act
		val result = mapper.toDto(ProjectModel().apply { this.counts = counts })

		// Assert
		assertEquals(4, result.counts?.vehicles)
		assertEquals(5, result.counts?.activities)
		assertEquals(6, result.counts?.activeAlerts)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should toDto convert ProjectModel to ProjectReaderDto`(
		project: ProjectModel,
		expectedTranslation: Int,
	) {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenReturn("translated")

		// Act
		val result = mapper.toDto(project)

		// Assert
		verify(translateService, times(expectedTranslation)).getMessage(
			code = "${PROJECT_OPTION_NAME_PREFIX}VEHICLE",
		)

		assertEquals(project.id, result.id)
		assertEquals(project.name, result.name)
		assertEquals(project.begin, result.begin)
		assertEquals(project.end, result.end)
		assertEquals(project.options?.size, result.options?.size)
		if ((project.options?.size ?: 0) > 0) {
			for (i in project.options!!.indices) {
				assertEquals(project.options!![i].name, result.options!![i].value)
			}
		}
		assertEquals(project.options?.size, result.options?.size)
		assertEquals(project.counts?.participants, result.counts?.participants)
		assertEquals(project.counts?.groups, result.counts?.groups)
		assertEquals(project.counts?.acceptedProfiles, result.counts?.acceptedProfiles)
		assertEquals(project.counts?.vehicles, result.counts?.vehicles)
		assertNull(result.counts?.activities)
		assertNull(result.counts?.activeAlerts)
		assertEquals(project.visible, result.visible)
		assertEquals(project.creation, result.creation)
		assertEquals(project.lastEdition, result.lastEdition)
	}
}
