package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfilesWriterDto
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class ProjectProfilesWriterDtoMapperTest {
	private val mapper = ProjectProfilesWriterDtoMapperImpl(CustomDateTimeWriterDtoMapperImpl())

	@Test
	fun `Should toModels normalize the emails and remove the duplicates`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val dto = ProjectProfilesWriterDto(
			emails = listOf(" John@Doe.FR ", "john@doe.fr", "jane@doe.fr"),
			role = "ROLE",
		)

		// Act
		val result = mapper.toModels(dto, projectId)

		// Assert
		assertEquals(listOf("john@doe.fr", "jane@doe.fr"), result.map { it.user!!.email })
		assertEquals(listOf("ROLE", "ROLE"), result.map { it.role })
		assertEquals(listOf(projectId, projectId), result.map { it.projectId })
		assertNull(result.first().user!!.id)
	}
}
