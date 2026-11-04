package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class GroupWriterDtoMapperTest {
	private val mapper = GroupWriterDtoMapper(CustomDateTimeWriterDtoMapper())

	@Test
	fun `Should toModel flag the members as unchanged when none are provided`() {
		// Act
		val result = mapper.toModel(GroupWriterDto(name = "name"), UUID.randomUUID())

		// Assert
		assertTrue(result.membersUnchanged)
		assertTrue(result.members.isEmpty())
	}

	@Test
	fun `Should toModel map the provided members, even an empty list`() {
		// Arrange
		val memberId = UUID.randomUUID()

		// Act
		val withMembers = mapper.toModel(GroupWriterDto(name = "name", members = listOf(memberId)), UUID.randomUUID())
		val emptied = mapper.toModel(GroupWriterDto(name = "name", members = emptyList()), UUID.randomUUID())

		// Assert
		assertFalse(withMembers.membersUnchanged)
		assertEquals(listOf(memberId), withMembers.members.map { it.id })
		assertFalse(emptied.membersUnchanged)
		assertTrue(emptied.members.isEmpty())
	}
}
