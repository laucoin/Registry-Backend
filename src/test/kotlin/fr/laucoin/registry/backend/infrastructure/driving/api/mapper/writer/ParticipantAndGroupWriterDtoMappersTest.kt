package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapperImpl
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ParticipantAndGroupWriterDtoMappersTest {
	private val ids = IdReferenceDtoMapperImpl()
	private val dates = CustomDateTimeWriterDtoMapperImpl()
	private val participantMapper = ParticipantWriterDtoMapperImpl(dates, ids)
	private val groupMapper = GroupWriterDtoMapperImpl(dates, ids)

	@Test
	fun `Should map a registered participant with its user and its groups`() {
		// Arrange
		val userId = UUID.randomUUID()
		val firstGroup = UUID.randomUUID()
		val secondGroup = UUID.randomUUID()
		val dto = ParticipantWriterDto(
			firstName = "Ada",
			lastName = "Lovelace",
			userId = userId,
			groupIds = listOf(firstGroup, secondGroup),
		)

		// Act
		val result = participantMapper.toModel(dto, defaultProjectId)

		// Assert
		assertEquals("Ada", result.firstName)
		assertEquals(REGISTERED, result.type)
		assertEquals(userId, result.user?.id)
		assertEquals(listOf(firstGroup, secondGroup), result.groups.map { it.id })
		assertEquals(defaultProjectId, result.projectId)
	}

	@Test
	fun `Should keep the groups empty and the user missing when they are not given`() {
		// Arrange
		val dto = ParticipantWriterDto(firstName = "Ada", userId = null, groupIds = null)

		// Act
		val result = participantMapper.toModel(dto, defaultProjectId)

		// Assert
		assertNull(result.user)
		assertEquals(emptyList<UUID>(), result.groups.map { it.id })
	}

	@Test
	fun `Should map a group with its members`() {
		// Arrange
		val first = UUID.randomUUID()
		val second = UUID.randomUUID()
		val dto = GroupWriterDto(name = "Wolves", members = listOf(first, second))

		// Act
		val result = groupMapper.toModel(dto, defaultProjectId)

		// Assert
		assertEquals("Wolves", result.name)
		assertEquals(listOf(first, second), result.members.map { it.id })
		assertEquals(defaultProjectId, result.projectId)
	}
}
