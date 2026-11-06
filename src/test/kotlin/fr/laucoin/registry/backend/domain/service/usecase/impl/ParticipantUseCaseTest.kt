package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.service.core.IParticipantSearchService
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantTimelineModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.core.IParticipantService
import fr.laucoin.registry.backend.domain.service.usecase.IParticipantUseCase
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ParticipantUseCaseTest {
	private val service: IParticipantService = mock()
	private val searchService: IParticipantSearchService = mock()
	private val useCase: IParticipantUseCase = ParticipantUseCase(service, searchService)

	@Test
	fun `Should findParticipantsPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: ParticipantSearchParamModel = mock()
		val expected: Mono<PageModel<ParticipantModel>> = Mono.empty()
		whenever(service.findParticipantsPage(projectId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findParticipantsPage(projectId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findParticipantsPage(projectId, pageable, searchParams)
	}

	@Test
	fun `Should findBirthdays delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val expected: Flux<ParticipantModel> = Flux.empty()
		whenever(service.findBirthdays(projectId)).thenReturn(expected)

		// Act
		val result = useCase.findBirthdays(projectId)

		// Assert
		assertSame(expected, result)
		verify(service).findBirthdays(projectId)
	}

	@Test
	fun `Should findParticipantTimeline delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ParticipantTimelineModel> = Mono.empty()
		whenever(service.findParticipantTimeline(projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.findParticipantTimeline(projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).findParticipantTimeline(projectId, id)
	}

	@Test
	fun `Should findParticipantById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<ParticipantModel> = Mono.empty()
		whenever(service.findParticipantById(projectId, id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findParticipantById(projectId, id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(service).findParticipantById(projectId, id, isVisible)
	}

	@Test
	fun `Should searchUsersByText delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val query: String? = null
		val expected: Flux<UserModel> = Flux.empty()
		whenever(searchService.searchUsersByText(projectId, query)).thenReturn(expected)

		// Act
		val result = useCase.searchUsersByText(projectId, query)

		// Assert
		assertSame(expected, result)
		verify(searchService).searchUsersByText(projectId, query)
	}

	@Test
	fun `Should searchGroupsByText delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val query: String? = null
		val expected: Flux<GroupModel> = Flux.empty()
		whenever(searchService.searchGroupsByText(projectId, query)).thenReturn(expected)

		// Act
		val result = useCase.searchGroupsByText(projectId, query)

		// Assert
		assertSame(expected, result)
		verify(searchService).searchGroupsByText(projectId, query)
	}

	@Test
	fun `Should findParticipantMovementsPage delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: MovementSearchParamModel = mock()
		val expected: Mono<PageModel<MovementModel>> = Mono.empty()
		whenever(service.findParticipantMovementsPage(currentUser, projectId, id, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findParticipantMovementsPage(currentUser, projectId, id, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(service).findParticipantMovementsPage(currentUser, projectId, id, pageable, searchParams)
	}

	@Test
	fun `Should createParticipant delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val participant: ParticipantModel = mock()
		val expected: Mono<ParticipantModel> = Mono.empty()
		whenever(service.createParticipant(currentUser, participant)).thenReturn(expected)

		// Act
		val result = useCase.createParticipant(currentUser, participant)

		// Assert
		assertSame(expected, result)
		verify(service).createParticipant(currentUser, participant)
	}

	@Test
	fun `Should updateParticipantById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val participant: ParticipantModel = mock()
		val expected: Mono<ParticipantModel> = Mono.empty()
		whenever(service.updateParticipantById(currentUser, projectId, id, participant)).thenReturn(expected)

		// Act
		val result = useCase.updateParticipantById(currentUser, projectId, id, participant)

		// Assert
		assertSame(expected, result)
		verify(service).updateParticipantById(currentUser, projectId, id, participant)
	}

	@Test
	fun `Should disableParticipantById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ParticipantModel> = Mono.empty()
		whenever(service.disableParticipantById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.disableParticipantById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).disableParticipantById(currentUser, projectId, id)
	}

	@Test
	fun `Should enableParticipantById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<ParticipantModel> = Mono.empty()
		whenever(service.enableParticipantById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.enableParticipantById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).enableParticipantById(currentUser, projectId, id)
	}

	@Test
	fun `Should deleteParticipantById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteParticipantById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteParticipantById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteParticipantById(currentUser, projectId, id)
	}
}
