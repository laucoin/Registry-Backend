package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.service.core.IMovementQueryService
import fr.laucoin.registry.backend.domain.service.core.IMovementSearchService
import fr.laucoin.registry.backend.domain.service.core.IMovementService
import fr.laucoin.registry.backend.domain.service.usecase.IMovementUseCase
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

class MovementUseCaseTest {
	private val queryService: IMovementQueryService = mock()
	private val searchService: IMovementSearchService = mock()
	private val service: IMovementService = mock()
	private val useCase: IMovementUseCase = MovementUseCase(queryService, searchService, service)

	@Test
	fun `Should findMovementsPage delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: MovementSearchParamModel = mock()
		val expected: Mono<PageModel<MovementModel>> = Mono.empty()
		whenever(queryService.findMovementsPage(currentUser, projectId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findMovementsPage(currentUser, projectId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(queryService).findMovementsPage(currentUser, projectId, pageable, searchParams)
	}

	@Test
	fun `Should findCurrentMovementsPage delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: MovementSearchParamModel = mock()
		val expected: Mono<PageModel<MovementModel>> = Mono.empty()
		whenever(queryService.findCurrentMovementsPage(currentUser, projectId, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findCurrentMovementsPage(currentUser, projectId, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(queryService).findCurrentMovementsPage(currentUser, projectId, pageable, searchParams)
	}

	@Test
	fun `Should findMovementsContent delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val movementIds: List<UUID> = emptyList()
		val expected: Flux<Pair<UUID, List<MovementContentModel>>> = Flux.empty()
		whenever(queryService.findMovementsContent(projectId, movementIds)).thenReturn(expected)

		// Act
		val result = useCase.findMovementsContent(projectId, movementIds)

		// Assert
		assertSame(expected, result)
		verify(queryService).findMovementsContent(projectId, movementIds)
	}

	@Test
	fun `Should findCurrentMovementsContent delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val movementIds: List<UUID> = emptyList()
		val expected: Flux<Pair<UUID, List<MovementContentModel>>> = Flux.empty()
		whenever(queryService.findCurrentMovementsContent(projectId, movementIds)).thenReturn(expected)

		// Act
		val result = useCase.findCurrentMovementsContent(projectId, movementIds)

		// Assert
		assertSame(expected, result)
		verify(queryService).findCurrentMovementsContent(projectId, movementIds)
	}

	@Test
	fun `Should findMovementById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val isVisible: Boolean? = null
		val expected: Mono<MovementModel> = Mono.empty()
		whenever(queryService.findMovementById(projectId, id, isVisible)).thenReturn(expected)

		// Act
		val result = useCase.findMovementById(projectId, id, isVisible)

		// Assert
		assertSame(expected, result)
		verify(queryService).findMovementById(projectId, id, isVisible)
	}

	@Test
	fun `Should searchParticipantsAndGroupsByText delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val type: ParticipantTypeEnum = ParticipantTypeEnum.entries.first()
		val query: String? = null
		val at: ZonedDateTime = ZonedDateTime.now()
		val all: Boolean = true
		val expected: Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>> = Mono.empty()
		whenever(searchService.searchParticipantsAndGroupsByText(projectId, type, query, at, all)).thenReturn(expected)

		// Act
		val result = useCase.searchParticipantsAndGroupsByText(projectId, type, query, at, all)

		// Assert
		assertSame(expected, result)
		verify(searchService).searchParticipantsAndGroupsByText(projectId, type, query, at, all)
	}

	@Test
	fun `Should searchVehiclesByText delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val query: String? = null
		val expected: Flux<VehicleModel> = Flux.empty()
		whenever(searchService.searchVehiclesByText(projectId, query)).thenReturn(expected)

		// Act
		val result = useCase.searchVehiclesByText(projectId, query)

		// Assert
		assertSame(expected, result)
		verify(searchService).searchVehiclesByText(projectId, query)
	}

	@Test
	fun `Should searchReasonsByText delegate to the service`() {
		// Arrange
		val contentType: ParticipantTypeEnum = ParticipantTypeEnum.entries.first()
		val type: MovementTypeEnum = MovementTypeEnum.entries.first()
		val expected: Flux<MovementReasonEnum> = Flux.empty()
		whenever(searchService.searchReasonsByText(contentType, type)).thenReturn(expected)

		// Act
		val result = useCase.searchReasonsByText(contentType, type)

		// Assert
		assertSame(expected, result)
		verify(searchService).searchReasonsByText(contentType, type)
	}

	@Test
	fun `Should searchActivitiesByText delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val contentType: ParticipantTypeEnum = ParticipantTypeEnum.entries.first()
		val query: String? = null
		val expected: Flux<ActivityModel> = Flux.empty()
		whenever(searchService.searchActivitiesByText(currentUser, projectId, contentType, query)).thenReturn(expected)

		// Act
		val result = useCase.searchActivitiesByText(currentUser, projectId, contentType, query)

		// Assert
		assertSame(expected, result)
		verify(searchService).searchActivitiesByText(currentUser, projectId, contentType, query)
	}

	@Test
	fun `Should findMovementCommunicationsPage delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val pageable: PageableModel = mock()
		val searchParams: CommunicationSearchParamModel = mock()
		val expected: Mono<PageModel<CommunicationModel>> = Mono.empty()
		whenever(queryService.findMovementCommunicationsPage(projectId, id, pageable, searchParams)).thenReturn(expected)

		// Act
		val result = useCase.findMovementCommunicationsPage(projectId, id, pageable, searchParams)

		// Assert
		assertSame(expected, result)
		verify(queryService).findMovementCommunicationsPage(projectId, id, pageable, searchParams)
	}

	@Test
	fun `Should findPlannedMovements delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val type: MovementTypeEnum = MovementTypeEnum.entries.first()
		val date: LocalDate? = null
		val expected: Flux<PlannedMovementModel> = Flux.empty()
		whenever(searchService.findPlannedMovements(projectId, type, date)).thenReturn(expected)

		// Act
		val result = useCase.findPlannedMovements(projectId, type, date)

		// Assert
		assertSame(expected, result)
		verify(searchService).findPlannedMovements(projectId, type, date)
	}

	@Test
	fun `Should findParticipantsStatus delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val expected: Mono<ProjectStatusModel> = Mono.empty()
		whenever(queryService.findParticipantsStatus(projectId)).thenReturn(expected)

		// Act
		val result = useCase.findParticipantsStatus(projectId)

		// Assert
		assertSame(expected, result)
		verify(queryService).findParticipantsStatus(projectId)
	}

	@Test
	fun `Should findVehiclesStatus delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val expected: Mono<VehicleStatusModel> = Mono.empty()
		whenever(queryService.findVehiclesStatus(projectId)).thenReturn(expected)

		// Act
		val result = useCase.findVehiclesStatus(projectId)

		// Assert
		assertSame(expected, result)
		verify(queryService).findVehiclesStatus(projectId)
	}

	@Test
	fun `Should createMovement delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val movement: MovementModel = mock()
		val newGuests: List<ParticipantModel> = emptyList()
		val expected: Mono<MovementModel> = Mono.empty()
		whenever(service.createMovement(currentUser, movement, newGuests)).thenReturn(expected)

		// Act
		val result = useCase.createMovement(currentUser, movement, newGuests)

		// Assert
		assertSame(expected, result)
		verify(service).createMovement(currentUser, movement, newGuests)
	}

	@Test
	fun `Should updateMovementById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val movement: MovementModel = mock()
		val newGuests: List<ParticipantModel> = emptyList()
		val expected: Mono<MovementModel> = Mono.empty()
		whenever(service.updateMovementById(currentUser, projectId, id, movement, newGuests)).thenReturn(expected)

		// Act
		val result = useCase.updateMovementById(currentUser, projectId, id, movement, newGuests)

		// Assert
		assertSame(expected, result)
		verify(service).updateMovementById(currentUser, projectId, id, movement, newGuests)
	}

	@Test
	fun `Should disableMovementById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<MovementModel> = Mono.empty()
		whenever(service.disableMovementById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.disableMovementById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).disableMovementById(currentUser, projectId, id)
	}

	@Test
	fun `Should enableMovementById delegate to the service`() {
		// Arrange
		val currentUser: CurrentUserModel = mock()
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<MovementModel> = Mono.empty()
		whenever(service.enableMovementById(currentUser, projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.enableMovementById(currentUser, projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).enableMovementById(currentUser, projectId, id)
	}

	@Test
	fun `Should deleteMovementById delegate to the service`() {
		// Arrange
		val projectId: UUID = UUID.randomUUID()
		val id: UUID = UUID.randomUUID()
		val expected: Mono<Unit> = Mono.empty()
		whenever(service.deleteMovementById(projectId, id)).thenReturn(expected)

		// Act
		val result = useCase.deleteMovementById(projectId, id)

		// Assert
		assertSame(expected, result)
		verify(service).deleteMovementById(projectId, id)
	}
}
