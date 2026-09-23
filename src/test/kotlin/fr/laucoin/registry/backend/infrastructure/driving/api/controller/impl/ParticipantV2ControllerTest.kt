package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantDataExportModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantDataExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantDataExportReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ParticipantV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IParticipantService

	@MockitoBean
	private lateinit var readerMapper: ParticipantReaderDtoMapper

	@MockitoBean
	private lateinit var partialUserReaderMapper: PartialUserReaderDtoMapper

	@MockitoBean
	private lateinit var groupReaderMapper: GroupWithoutMemberReaderDtoMapper

	@MockitoBean
	private lateinit var movementReaderMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var dataExportReaderMapper: ParticipantDataExportReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ParticipantWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/participants"
	}

	@Test
	fun `Should findParticipants call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findParticipantsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findParticipantsPage(
			projectId,
			pageable,
			ParticipantSearchParamModel(textSearched = null, isMajor = null, typeSearched = null, visibilitySearched = null, statusSearched = null, dateTimeSearched = null),
			emptyList(),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should findParticipants return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findParticipants return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findBirthdays return 200`() {
		// Arrange
		whenever(service.findBirthdays(any(), any())).thenReturn(Flux.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/birthday", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findBirthdays(projectId, 5)
	}

	@Test
	fun `Should findArrivingToday return 200`() {
		// Arrange
		whenever(service.findArrivingToday(any(), any())).thenReturn(Flux.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/arrivals-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findArrivingToday(projectId, 5)
	}

	@Test
	fun `Should findDepartingToday return 200`() {
		// Arrange
		whenever(service.findDepartingToday(any(), any())).thenReturn(Flux.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/departures-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findDepartingToday(projectId, 5)
	}

	@Test
	fun `Should findParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findParticipantById(any(), any(), anyOrNull())).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)
		verify(service).findParticipantById(projectId, uuid, visibilitySearched = null)
	}

	@Test
	fun `Should searchUsers rename textSearched to q`() {
		// Arrange
		val searched = "text"
		whenever(service.searchUsersByText(any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/search/users", listOf(projectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchUsersByText(projectId, searched)
	}

	@Test
	fun `Should searchGroups rename textSearched to q`() {
		// Arrange
		val searched = "text"
		whenever(service.searchGroupsByText(any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/search/groups", listOf(projectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchGroupsByText(projectId, searched)
	}

	@Test
	fun `Should findParticipantMovements drop the Searched suffix and call service`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val searchParams = MovementSearchParamModel(visibilitySearched = true, typeSearched = null)
		val page = PageModel(pageable, totalElements = 0, emptyList<MovementModel>())
		whenever(service.findParticipantMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/movements", listOf(projectId, uuid), listOf(Pair("visible", true))))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)
		verify(service).findParticipantMovementsPage(projectId, uuid, pageable, searchParams)
	}

	@Test
	fun `Should exportParticipantDataById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.exportParticipantData(any(), any())).thenReturn(Mono.just(ParticipantDataExportModel()))
		whenever(dataExportReaderMapper.toDto(any())).thenReturn(ParticipantDataExportReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/data-export", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantDataExportReaderDto>(OK)
		verify(service).exportParticipantData(projectId, uuid)
	}

	@Test
	fun `Should createParticipant return 201 with a Location header`() {
		// Arrange
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)
		val createdId = UUID.randomUUID()
		whenever(service.createParticipant(any(), any())).thenReturn(Mono.just(ParticipantModel().apply { id = createdId }))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(ParticipantReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$projectId/participants/$createdId"))

		verify(service).createParticipant(any(), any())
		verify(writerMapper).toModel(participant, projectId)
	}

	@Test
	fun `Should createParticipant return 400`() {
		// Arrange
		val participant = ParticipantWriterDto(lastName = "DOE", birthday = LocalDate.EPOCH)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARTICIPANT_FIRST_NAME_NULL_OR_BLANK)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateParticipant return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)
		whenever(service.updateParticipantById(any(), any(), any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)
		verify(service).updateParticipantById(any(), eq(projectId), eq(uuid), any())
	}

	@Test
	fun `Should disableParticipantById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.disableParticipantById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)
		verify(service).disableParticipantById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should enableParticipantById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.enableParticipantById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)
		verify(service).enableParticipantById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should deleteParticipantById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.deleteParticipantById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verify(service).deleteParticipantById(any(), eq(projectId), eq(uuid))
	}
}
