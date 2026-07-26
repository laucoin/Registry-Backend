package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CreatedProjectProfilesReaderDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals

class CreatedProjectProfilesReaderDtoMapperTest {
	private val mapper = CreatedProjectProfilesReaderDtoMapper()

	private companion object {
		private val uuid1 = UUID.randomUUID()
		private val uuid2 = UUID.randomUUID()
		private val uuid3 = UUID.randomUUID()

		private val model = Pair(listOf(uuid1, uuid2), listOf(uuid3))
		private val dto = CreatedProjectProfilesReaderDto(listOf(uuid1, uuid2), listOf(uuid3))

		@JvmStatic
		fun `Pair to CreatedProjectProfilesReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1, 1),
			)
		}
	}

	@ParameterizedTest
	@MethodSource("Pair to CreatedProjectProfilesReaderDto data")
	fun `Should toDto convert Pair to CreatedProjectProfilesReaderDto`(
		model: Pair<List<UUID>, List<UUID>>, dto: CreatedProjectProfilesReaderDto
	) {
		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(dto, result)
	}

	@ParameterizedTest
	@MethodSource("Pair to CreatedProjectProfilesReaderDto data")
	fun `Should toDto convert Pair list to CreatedProjectProfilesReaderDto list`(
		model: Pair<List<UUID>, List<UUID>>, dto: CreatedProjectProfilesReaderDto
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models)

		// Assert
		assertEquals(dtos, result)
	}

	@ParameterizedTest
	@MethodSource("Pair to CreatedProjectProfilesReaderDto data")
	fun `Should toDto convert Pair page to CreatedProjectProfilesReaderDto page`(
		model: Pair<List<UUID>, List<UUID>>, dto: CreatedProjectProfilesReaderDto
	) {
		// Arrange
		val modelPage = PageModel(
			pageNumber = 0,
			pageSize = 10,
			totalPages = 1,
			totalElements = 1,
			content = listOf(model),
		)
		val dtoPage = PageModel(
			pageNumber = 0,
			pageSize = 10,
			totalPages = 1,
			totalElements = 1,
			content = listOf(dto),
			lastRefresh = modelPage.lastRefresh,
		)

		// Act
		val result = mapper.toDtoPage(modelPage)

		// Assert
		assertEquals(dtoPage, result)
	}
}
