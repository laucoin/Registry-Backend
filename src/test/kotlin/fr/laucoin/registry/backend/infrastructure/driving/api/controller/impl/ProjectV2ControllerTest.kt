package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.OffsetTime
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
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class ProjectV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IProjectService

	@MockitoBean
	private lateinit var readerMapper: ProjectReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ProjectWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects"
	}

	@Test
	fun `Should findProjects call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ProjectModel()))
		whenever(service.findProjectsPage(any(), any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findProjectsPage(
			any(),
			eq(pageable),
			eq(true),
			eq(ProjectSearchParamModel(textSearched = null, visibilitySearched = null, dateTimeSearched = null)),
			eq(emptyList()),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should findProjects thread favorite query param to search params`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ProjectModel()))
		whenever(service.findProjectsPage(any(), any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("favorite", true))))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findProjectsPage(
			any(),
			eq(pageable),
			eq(true),
			eq(ProjectSearchParamModel(textSearched = null, visibilitySearched = null, dateTimeSearched = null, favoriteSearched = true)),
			eq(emptyList()),
		)
	}

	@Test
	fun `Should findProjects return 403 when withProfile is false without REGISTRY_PROJECT_R`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("withProfile", false))))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findProjects return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findProjects return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findProjectById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findProjectById(any(), anyOrNull())).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)
		verify(service).findProjectById(uuid, visibilitySearched = null)
	}

	@Test
	fun `Should createProject return 201 with a Location header`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = emptyList(),
		)
		val createdId = UUID.randomUUID()
		whenever(service.createProject(any(), any())).thenReturn(Mono.just(ProjectModel().apply { id = createdId }))
		whenever(writerMapper.toModel(any())).thenReturn(ProjectModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_C)
			.post()
			.uri(BASE_URL)
			.bodyValue(project)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(ProjectReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$createdId"))

		verify(service).createProject(any(), any())
	}

	@Test
	fun `Should createProject return 400`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = emptyList(),
		)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(BASE_URL)
			.bodyValue(project)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PROJECT_NAME_NULL_OR_BLANK)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateProjectById return 200`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = emptyList(),
		)
		whenever(service.updateProjectById(any(), any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(writerMapper.toModel(any())).thenReturn(ProjectModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.bodyValue(project)
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)
		verify(service).updateProjectById(any(), eq(projectId), any())
	}

	@Test
	fun `Should disableProjectById use POST and return 200`() {
		// Arrange
		whenever(service.disableProjectById(any(), eq(projectId))).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)
		verify(service).disableProjectById(any(), eq(projectId))
	}

	@Test
	fun `Should enableProjectById use POST and return 200`() {
		// Arrange
		whenever(service.enableProjectById(any(), eq(projectId))).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)
		verify(service).enableProjectById(any(), eq(projectId))
	}

	@Test
	fun `Should deleteProjectById return 204`() {
		// Arrange
		whenever(service.deleteProjectById(any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verify(service).deleteProjectById(projectId)
	}
}
