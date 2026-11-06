package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.ExportConst
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserExportModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ExportReaderDtoMapperTest {
	private val mapper = ExportReaderDtoMapperImpl()
	private val now = ZonedDateTime.now(ZoneOffset.UTC)

	private fun participantExport(subjectUserId: UUID?, editorId: UUID?): ParticipantExportModel {
		val participant = ParticipantModel(
			firstName = "Ada",
			type = REGISTERED,
			groups = listOf(GroupModel(name = "Wolves"), GroupModel(name = null)),
			user = subjectUserId?.let { UserModel().apply { id = it } },
		).apply {
			id = UUID.randomUUID()
			creation = HistoryModel(now, HistoryUserModel(editorId, null, null, null))
		}
		val movement = MovementModel(type = IN).apply { activity = ActivityModel(name = "Hike") }
		return ParticipantExportModel(now, "Project", participant, listOf(movement))
	}

	@Test
	fun `Should export a participant with its group names, its movements and the schema constants`() {
		// Arrange
		val subjectId = UUID.randomUUID()
		val model = participantExport(subjectId, subjectId)

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(ExportConst.SCHEMA_VERSION, result.schemaVersion)
		assertEquals(ExportConst.SUBJECT_PARTICIPANT, result.subjectType)
		assertEquals("Ada", result.participant.firstName)
		assertEquals("Project", result.participant.projectName)
		assertEquals("REGISTERED", result.participant.type)
		assertEquals(listOf("Wolves"), result.participant.groups)
		assertEquals("IN", result.participant.movements.single().type)
		assertEquals("Hike", result.participant.movements.single().activity)
	}

	@Test
	fun `Should flag a modification made by the data subject only`() {
		// Arrange
		val subjectId = UUID.randomUUID()
		val byTheSubject = participantExport(subjectId, subjectId)
		val byAnotherUser = participantExport(subjectId, UUID.randomUUID())
		val withoutSubject = participantExport(null, UUID.randomUUID())

		// Act
		val own = mapper.toDto(byTheSubject).participant.creation
		val foreign = mapper.toDto(byAnotherUser).participant.creation
		val anonymous = mapper.toDto(withoutSubject).participant.creation

		// Assert
		assertTrue(own!!.byDataSubject)
		assertFalse(foreign!!.byDataSubject)
		assertFalse(anonymous!!.byDataSubject)
		assertNull(mapper.toDto(byTheSubject).participant.lastEdition)
	}

	@Test
	fun `Should export a user with its preferences, its profiles and the blocked flag`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val user = UserModel(firstName = "Ada", email = "ada@sgdf.fr").apply { id = UUID.randomUUID(); visible = false }
		val withProject = ProjectProfileModel(role = "ROLE").apply { project = ProjectModel(name = "Project").apply { id = projectId } }
		val withoutProject = ProjectProfileModel(role = "ROLE").apply { this.projectId = projectId }
		val model = UserExportModel(
			now,
			user,
			PreferencesModel(theme = ThemeEnum.DARK, language = "fr"),
			listOf(withProject, withoutProject),
			emptyList(),
		)

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(ExportConst.SUBJECT_USER, result.subjectType)
		assertTrue(result.user.blocked)
		assertEquals("USER", result.user.type)
		assertEquals("DARK", result.preferences?.theme)
		assertEquals("Project", result.projectProfiles.first().projectName)
		assertEquals(projectId, result.projectProfiles[0].projectId)
		assertEquals(projectId, result.projectProfiles[1].projectId)
		assertNull(result.projectProfiles[1].projectName)
	}
}
