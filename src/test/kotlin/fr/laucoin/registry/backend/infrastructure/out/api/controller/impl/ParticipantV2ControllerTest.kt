package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_COLLECTION_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum.FIRST_NAME
import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum.LAST_NAME
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.test.ModelExt.commonGroup
import fr.laucoin.registry.backend.test.ModelExt.commonMovement
import fr.laucoin.registry.backend.test.ModelExt.commonParticipant
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class ParticipantV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IParticipantService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/participants"
	}

	private fun participantPage(): Mono<PageModel<ParticipantModel>> =
		Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonParticipant())))

	@Test
	fun `Should findParticipants return 200 with the v2 list grammar`() {
		// Arrange
		whenever(service.findParticipantsPage(any(), any(), any(), any())).thenReturn(participantPage())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "john"),
						Pair("visible", true),
						Pair("sort", "lastName,-firstName"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<ParticipantReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<ParticipantSortFieldEnum>>>()
		verify(service).findParticipantsPage(eq(projectId), pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(LAST_NAME), SortModel(FIRST_NAME, descending = true)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findParticipants reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "socialSecurityNumber"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findParticipants return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should disableParticipantById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.disableParticipantById(any(), any(), any())).thenReturn(Mono.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)
		verify(service).disableParticipantById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should findLinkableUsers expose the eligibility sub-collection with q`() {
		// Arrange
		whenever(service.searchUsersByText(any(), anyOrNull())).thenReturn(Flux.just(commonUser()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/linkable-users", listOf(projectId), listOf(Pair("q", "john"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchUsersByText(projectId, "john")
	}

	private fun participant() = ParticipantWriterDto(
		firstName = "John",
		lastName = "DOE",
		birthday = LocalDate.of(2000, 1, 1),
	)

	@Test
	fun `Should findBirthdays pass the default limit to the service`() {
		// Arrange
		whenever(service.findBirthdays(any(), any())).thenReturn(Flux.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/birthdays", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findBirthdays(projectId, DEFAULT_COLLECTION_LIMIT)
	}

	@Test
	fun `Should findBirthdays return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/birthdays", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findArrivingToday pass the default limit to the service`() {
		// Arrange
		whenever(service.findArrivingToday(any(), any())).thenReturn(Flux.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/arriving-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findArrivingToday(projectId, DEFAULT_COLLECTION_LIMIT)
	}

	@Test
	fun `Should findArrivingToday return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/arriving-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findDepartingToday pass the default limit to the service`() {
		// Arrange
		whenever(service.findDepartingToday(any(), any())).thenReturn(Flux.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/departing-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findDepartingToday(projectId, DEFAULT_COLLECTION_LIMIT)
	}

	@Test
	fun `Should findDepartingToday return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/departing-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findParticipantById return 200 with the mapped participant`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findParticipantById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		val participant = result.body<ParticipantReaderDto>(OK)
		assertEquals(participantId, participant?.id)
		verify(service).findParticipantById(projectId, id, visibilitySearched = null)
	}

	@Test
	fun `Should findParticipantById return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findLinkableGroups expose the eligibility sub-collection with q`() {
		// Arrange
		whenever(service.searchGroupsByText(any(), anyOrNull())).thenReturn(Flux.just(commonGroup()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/linkable-groups", listOf(projectId), listOf(Pair("q", "alpha"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchGroupsByText(projectId, "alpha")
	}

	@Test
	fun `Should findLinkableGroups return 403 without the metadata authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/linkable-groups", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findParticipantMovements return 200 with the movements page`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findParticipantMovementsPage(any(), any(), any(), any())).thenReturn(
			Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonMovement()))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/movements", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<PageModel<MovementReaderDto>>(OK)
		verify(service).findParticipantMovementsPage(eq(projectId), eq(id), eq(PageableModel(0, 20)), any())
	}

	@Test
	fun `Should findParticipantMovements return 403 without the history authority`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/movements", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should createParticipant return 200 and delegate to the service`() {
		// Arrange
		whenever(service.createParticipant(any(), any())).thenReturn(Mono.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participant())
			.exchange()

		// Assert
		val created = result.body<ParticipantReaderDto>(OK)
		assertEquals(participantId, created?.id)
		verify(service).createParticipant(any(), any())
	}

	@Test
	fun `Should createParticipant return 403 without the create authority`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participant())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateParticipantById return 200 and delegate to the service`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateParticipantById(any(), any(), any(), any())).thenReturn(Mono.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(participant())
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)
		verify(service).updateParticipantById(any(), eq(projectId), eq(id), any())
	}

	@Test
	fun `Should updateParticipantById return 403 without the update authority`() {
		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.bodyValue(participant())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should enableParticipantById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.enableParticipantById(any(), any(), any())).thenReturn(Mono.just(commonParticipant()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)
		verify(service).enableParticipantById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should enableParticipantById return 403 without the update authority`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteParticipantById return 200 and delegate to the service`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteParticipantById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)
		verify(service).deleteParticipantById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should deleteParticipantById return 403 without the delete authority`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}
}
