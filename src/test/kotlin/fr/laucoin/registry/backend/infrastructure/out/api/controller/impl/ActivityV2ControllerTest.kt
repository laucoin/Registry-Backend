package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum.DURATION
import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum.NAME
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.commonMovement
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
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals

class ActivityV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IActivityService

	@MockitoBean
	private lateinit var readerMapper: ActivityReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/activities"
	}

	@Test
	fun `Should findActivities return 200 with the v2 list grammar`() {
		// Arrange
		val pageable = PageableModel(20, 10)
		val page = PageModel(pageable, totalElements = 1, listOf(ActivityModel()))
		whenever(service.findActivitiesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ActivityReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "climbing"),
						Pair("visible", true),
						Pair("available", true),
						Pair("sort", "name,duration"),
						Pair("direction", "DESC"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<ActivityReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<ActivitySortFieldEnum>>>()
		verify(service).findActivitiesPage(eq(projectId), pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(NAME, descending = true), SortModel(DURATION, descending = true)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findActivities reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "secret"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findActivities return 403 without the read authority`() {
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
	fun `Should disableActivityById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.disableActivityById(any(), any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)
		verify(service).disableActivityById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should enableActivityById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.enableActivityById(any(), any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)
		verify(service).enableActivityById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should findActivityById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findActivityById(any(), any(), anyOrNull())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)
		verify(service).findActivityById(projectId, id, null)
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should findActivityById return 403 without the read authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findActivityMovements return 200 with the v2 list grammar`() {
		// Arrange
		val id = UUID.randomUUID()
		val page = PageModel(PageableModel(20, 10), totalElements = 1, listOf(commonMovement()))
		whenever(service.findActivityMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_ACTIVITY_HISTORY_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY),
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(projectId, id),
					listOf(Pair("page", 2), Pair("size", 10)),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<MovementReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		verify(service).findActivityMovementsPage(eq(projectId), eq(id), pageableCaptor.capture(), any())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
	}

	@Test
	fun `Should findActivityMovements return 403 without the history read authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/movements", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should createActivity return 200`() {
		// Arrange
		val activity = ActivityWriterDto(name = "Climbing")
		whenever(service.createActivity(any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_C), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)
		verify(service).createActivity(any(), any())
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should createActivity return 403 without the create authority`() {
		// Arrange
		val activity = ActivityWriterDto(name = "Climbing")

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateActivityById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		val activity = ActivityWriterDto(name = "Climbing")
		whenever(service.updateActivityById(any(), any(), any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)
		verify(service).updateActivityById(any(), eq(projectId), eq(id), any())
	}

	@Test
	fun `Should updateActivityById return 403 without the update authority`() {
		// Arrange
		val id = UUID.randomUUID()
		val activity = ActivityWriterDto(name = "Climbing")

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteActivityById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteActivityById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_D), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)
		verify(service).deleteActivityById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should deleteActivityById return 403 without the delete authority`() {
		// Arrange
		val id = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}
}
