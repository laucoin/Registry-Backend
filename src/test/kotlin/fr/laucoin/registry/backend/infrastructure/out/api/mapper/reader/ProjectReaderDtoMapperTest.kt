package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.VEHICLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals

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
						visible = true
						creation = HistoryModel()
						lastEdition = HistoryModel()
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
						creation = HistoryModel()
						lastEdition = HistoryModel()
					},
					0,
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should toDto convert ProjectModel to ProjectReaderDto`(
		project: ProjectModel,
		expectedTranslation: Int,
	) {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")

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
		assertEquals(project.visible, result.visible)
		assertEquals(project.creation, result.creation)
		assertEquals(project.lastEdition, result.lastEdition)
	}
}
