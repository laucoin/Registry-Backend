package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ActivityWriterDtoMapper
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
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
import reactor.core.publisher.Mono

class ActivityV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IActivityService

	@MockitoBean
	private lateinit var readerMapper: ActivityReaderDtoMapper

	@MockitoBean
	private lateinit var movementReaderMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ActivityWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/activities"
	}

	@Test
	fun `Should findActivities call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ActivityModel()))
		whenever(service.findActivitiesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findActivitiesPage(
			projectId,
			pageable,
			ActivitySearchParamModel(textSearched = null, visibilitySearched = null, availabilitySearched = null, dateTimeSearched = null),
			emptyList(),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findActivities return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findActivities return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findActivityById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findActivityById(any(), any(), anyOrNull())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).findActivityById(projectId, uuid, visibilitySearched = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(movementReaderMapper)
	}

	@Test
	fun `Should findActivityMovements drop the Searched suffix and call service`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val searchParams = MovementSearchParamModel(visibilitySearched = true, typeSearched = IN)
		val page = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findActivityMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(movementReaderMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_HISTORY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(projectId, uuid),
					listOf(Pair("visible", true), Pair("type", IN)),
				)
			)
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findActivityMovementsPage(projectId, uuid, pageable, searchParams)
		verify(movementReaderMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createActivity return 201 with a Location header`() {
		// Arrange
		val activity = ActivityWriterDto(name = "Activity 1")
		val createdId = UUID.randomUUID()

		whenever(service.createActivity(any(), any())).thenReturn(Mono.just(ActivityModel().apply { id = createdId }))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto().apply { id = createdId })
		whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_C), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(ActivityReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$projectId/activities/$createdId"))

		verify(service).createActivity(any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(activity, projectId)
		verifyNoInteractions(movementReaderMapper)
	}

	@Test
	fun `Should createActivity return 400`() {
		// Arrange
		val activity = ActivityWriterDto(name = null)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, ACTIVITY_NAME_NULL_OR_BLANK)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateActivity return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val activity = ActivityWriterDto(name = "Activity 1")

		whenever(service.updateActivityById(any(), any(), any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).updateActivityById(any(), eq(projectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verify(writerMapper).toModel(activity, projectId)
	}

	@Test
	fun `Should disableActivityById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.disableActivityById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).disableActivityById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableActivityById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableActivityById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).enableActivityById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteActivityById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteActivityById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_D), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteActivityById(any(), eq(projectId), eq(uuid))
	}
}
