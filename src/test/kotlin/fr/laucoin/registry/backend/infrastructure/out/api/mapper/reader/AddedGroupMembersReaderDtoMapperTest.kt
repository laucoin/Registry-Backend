package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AddedGroupMembersReaderDto
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AddedGroupMembersReaderDtoMapperTest {
	private val mapper = AddedGroupMembersReaderDtoMapper()

	private companion object {
		private val uuid1 = UUID.randomUUID()
		private val uuid2 = UUID.randomUUID()
		private val uuid3 = UUID.randomUUID()

		private val model = Pair(listOf(uuid1, uuid2), listOf(uuid3))
		private val dto = AddedGroupMembersReaderDto(listOf(uuid1, uuid2), listOf(uuid3))

		@JvmStatic
		fun `Pair to AddedGroupMembersReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1, 1),
			)
		}
	}

	@ParameterizedTest
	@MethodSource("Pair to AddedGroupMembersReaderDto data")
	fun `Should toDto convert Pair to AddedGroupMembersReaderDto`(
		model: Pair<List<UUID>, List<UUID>>, dto: AddedGroupMembersReaderDto
	) {
		// Act
		val result = mapper.toDto(model, Locale.getDefault())

		// Assert
		assertEquals(dto, result)
	}

	@ParameterizedTest
	@MethodSource("Pair to AddedGroupMembersReaderDto data")
	fun `Should toDto convert Pair list to AddedGroupMembersReaderDto list`(
		model: Pair<List<UUID>, List<UUID>>, dto: AddedGroupMembersReaderDto
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models, Locale.getDefault())

		// Assert
		assertEquals(dtos, result)
	}

	@ParameterizedTest
	@MethodSource("Pair to AddedGroupMembersReaderDto data")
	fun `Should toDto convert Pair page to AddedGroupMembersReaderDto page`(
		model: Pair<List<UUID>, List<UUID>>, dto: AddedGroupMembersReaderDto
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
		val result = mapper.toDtoPage(modelPage, Locale.getDefault())

		// Assert
		assertEquals(dtoPage, result)
	}
}
