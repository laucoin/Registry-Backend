package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_EXPORT
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.IExportService
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.security.core.authority.SimpleGrantedAuthority
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ExportServiceTest {
	private val userPort: IUserPort = mock()
	private val preferencesPort: IPreferencesPort = mock()
	private val profilePort: IProjectProfilePort = mock()
	private val participantPort: IParticipantPort = mock()
	private val movementPort: IMovementPort = mock()
	private val projectPort: IProjectPort = mock()
	private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), UTC)
	private val service: IExportService = ExportService(
		userPort, preferencesPort, profilePort, participantPort, movementPort, projectPort, clock,
	)

	private val userId = UUID.randomUUID()
	private val projectId = UUID.randomUUID()
	private val participantId = UUID.randomUUID()

	private fun currentUser(id: UUID = UUID.randomUUID(), vararg authorities: String) =
		CurrentUserModel().apply {
			this.id = id
			authorities.forEach { getAuthorities().add(SimpleGrantedAuthority(it)) }
		}

	private fun participant(linkedUserId: UUID? = null) = ParticipantModel(
		firstName = "Jane",
		lastName = "Doe",
		user = linkedUserId?.let { UserModel().apply { id = it } },
	).apply {
		id = participantId
		projectId = this@ExportServiceTest.projectId
	}

	private fun stubParticipantDependencies() {
		whenever(projectPort.findById(eq(projectId), anyOrNull())).thenReturn(Mono.just(ProjectModel(name = "Camp")))
		whenever(movementPort.findPageByParticipantId(eq(projectId), eq(participantId), any(), any()))
			.thenReturn(Mono.just(PageModel(0, 200, 1, 1, listOf(MovementModel()))))
	}

	@Test
	fun `Should exportParticipant allow a Project administrator`() {
		// Arrange
		val admin = currentUser(authorities = arrayOf("${projectId}_$REGISTRY_PROJECT_PARTICIPANT_EXPORT"))
		whenever(participantPort.findById(eq(projectId), eq(participantId), anyOrNull())).thenReturn(Mono.just(participant()))
		stubParticipantDependencies()

		// Act
		val result = service.exportParticipant(admin, projectId, participantId)

		// Assert
		StepVerifier.create(result)
			.expectNextMatches { it.projectName == "Camp" && it.movements.size == 1 && it.participant.lastName == "Doe" }
			.verifyComplete()
	}

	@Test
	fun `Should exportParticipant allow the linked User`() {
		// Arrange
		val self = currentUser(userId)
		whenever(participantPort.findById(eq(projectId), eq(participantId), anyOrNull()))
			.thenReturn(Mono.just(participant(userId)))
		stubParticipantDependencies()

		// Act
		val result = service.exportParticipant(self, projectId, participantId)

		// Assert
		StepVerifier.create(result).expectNextCount(1).verifyComplete()
	}

	@Test
	fun `Should exportParticipant forbid any other User`() {
		// Arrange
		val other = currentUser()
		whenever(participantPort.findById(eq(projectId), eq(participantId), anyOrNull()))
			.thenReturn(Mono.just(participant(userId)))

		// Act
		val result = service.exportParticipant(other, projectId, participantId)

		// Assert
		StepVerifier.create(result)
			.expectErrorMatches { it is RegistryException && it.status == FORBIDDEN && it.code == NOT_ENOUGH_PERMISSION }
			.verify()
	}

	@Test
	fun `Should exportParticipant forbid a GUEST for a non administrator`() {
		// Arrange
		whenever(participantPort.findById(eq(projectId), eq(participantId), anyOrNull())).thenReturn(Mono.just(participant()))

		// Act
		val result = service.exportParticipant(currentUser(), projectId, participantId)

		// Assert
		StepVerifier.create(result).expectError(RegistryException::class.java).verify()
	}

	@Test
	fun `Should exportParticipant return 404 when the Participant does not exist`() {
		// Arrange
		whenever(participantPort.findById(eq(projectId), eq(participantId), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = service.exportParticipant(currentUser(), projectId, participantId)

		// Assert
		StepVerifier.create(result)
			.expectErrorMatches { it is RegistryException && it.status == NOT_FOUND && it.code == NOT_FOUND_WITH_GIVEN_IDENTIFIER }
			.verify()
	}

	@Test
	fun `Should exportUser gather the User, its preferences, profiles and every linked Participant`() {
		// Arrange
		val user = UserModel(firstName = "Jane", email = "jane@doe.fr").apply { id = userId }
		whenever(userPort.findById(eq(userId), anyOrNull())).thenReturn(Mono.just(user))
		whenever(preferencesPort.findByUserId(eq(userId), anyOrNull())).thenReturn(Mono.just(PreferencesModel()))
		whenever(profilePort.findProjectProfilesPageByUserId(eq(userId), any(), any()))
			.thenReturn(Mono.just(PageModel(0, 200, 1, 1, listOf(ProjectProfileModel()))))
		whenever(participantPort.findAllByUserId(userId)).thenReturn(Flux.just(participant(userId)))
		whenever(participantPort.findById(eq(projectId), eq(participantId), anyOrNull()))
			.thenReturn(Mono.just(participant(userId)))
		stubParticipantDependencies()

		// Act
		val result = service.exportUser(currentUser(), userId)

		// Assert
		StepVerifier.create(result)
			.expectNextMatches {
				it.user.email == "jane@doe.fr" && it.preferences != null &&
					it.projectProfiles.size == 1 && it.participants.size == 1
			}
			.verifyComplete()
	}

	@Test
	fun `Should exportUser read every page of profiles`() {
		// Arrange
		val user = UserModel().apply { id = userId }
		whenever(userPort.findById(eq(userId), anyOrNull())).thenReturn(Mono.just(user))
		whenever(preferencesPort.findByUserId(eq(userId), anyOrNull())).thenReturn(Mono.empty())
		whenever(participantPort.findAllByUserId(userId)).thenReturn(Flux.empty())
		whenever(profilePort.findProjectProfilesPageByUserId(eq(userId), any(), any()))
			.thenReturn(
				Mono.just(PageModel(0, 200, 2, 201, listOf(ProjectProfileModel()))),
				Mono.just(PageModel(1, 200, 2, 201, listOf(ProjectProfileModel()))),
			)

		// Act
		val result = service.exportUser(currentUser(), userId)

		// Assert
		StepVerifier.create(result)
			.expectNextMatches { it.projectProfiles.size == 2 && it.preferences == null }
			.verifyComplete()
		verify(profilePort, times(2)).findProjectProfilesPageByUserId(eq(userId), any(), any())
	}

	@Test
	fun `Should exportUser return 404 when the User does not exist`() {
		// Arrange
		whenever(userPort.findById(eq(userId), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = service.exportUser(currentUser(), userId)

		// Assert
		StepVerifier.create(result)
			.expectErrorMatches { it is RegistryException && it.status == NOT_FOUND }
			.verify()
	}
}
