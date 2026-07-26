package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_METADATA_R
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum.END_DATE
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum.NAME
import fr.laucoin.registry.backend.domain.model.OpenAlertProjectModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.OpenAlertProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.test.ModelExt.commonProject
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
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import kotlin.test.assertEquals

class ProjectV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IProjectService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects"
	}

	private fun projectPage(): Mono<PageModel<ProjectModel>> =
		Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonProject())))

	@Test
	fun `Should findProjects return 200 with the v2 list grammar`() {
		// Arrange
		whenever(service.findProjectsPage(any(), any(), any(), any(), any())).thenReturn(projectPage())

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					emptyList(),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "summer"),
						Pair("visible", true),
						Pair("sort", "name,-endDate"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<ProjectReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<ProjectSortFieldEnum>>>()
		verify(service).findProjectsPage(any(), pageableCaptor.capture(), eq(true), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(NAME), SortModel(END_DATE, descending = true)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findProjects reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("sort", "options"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findProjects return 403 without profile nor the read authority`() {
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
	fun `Should disableProjectById act as an explicit POST transition`() {
		// Arrange
		whenever(service.disableProjectById(any(), any())).thenReturn(Mono.just(commonProject()))

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
	fun `Should enableProjectById act as an explicit POST transition`() {
		// Arrange
		whenever(service.enableProjectById(any(), any())).thenReturn(Mono.just(commonProject()))

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
	fun `Should findProjectById return 200 with the read permission`() {
		// Arrange
		whenever(service.findProjectById(any(), anyOrNull())).thenReturn(Mono.just(commonProject()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)
		verify(service).findProjectById(projectId, visibilitySearched = null)
	}

	@Test
	fun `Should findProjectById return 403 without the read permission`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should getAvailableProjectOptions return 200 with the metadata read authority`() {
		// Arrange
		whenever(service.availableProjectOptions()).thenReturn(Flux.just(ACTIVITY))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_METADATA_R)
			.get()
			.uri(uriBuilder("$BASE_URL/options", emptyList(), emptyList()))
			.exchange()

		// Assert
		val options = result.body<List<ProjectOptionsReaderDto>>(OK)
		assertEquals(ACTIVITY, options?.first()?.value)
		verify(service).availableProjectOptions()
	}

	@Test
	fun `Should getAvailableProjectOptions return 403 without the metadata read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/options", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createProject return 200 with the create authority`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)
		whenever(service.createProject(any(), any())).thenReturn(Mono.just(commonProject()))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_C)
			.post()
			.uri(uriBuilder(BASE_URL, emptyList(), emptyList()))
			.bodyValue(project)
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)
		verify(service).createProject(any(), any())
	}

	@Test
	fun `Should createProject return 403 without the create authority`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, emptyList(), emptyList()))
			.bodyValue(project)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findOpenAlertProjects return 200 for the authenticated caller`() {
		// Arrange
		whenever(service.findOpenAlertProjects(any(), any()))
			.thenReturn(Flux.just(OpenAlertProjectModel(commonProject(), 4)))

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/open-alerts", emptyList(), listOf(Pair("limit", 3))))
			.exchange()

		// Assert
		val projects = result.body<List<OpenAlertProjectReaderDto>>(OK)
		assertEquals(projectId, projects?.first()?.id)
		assertEquals(4L, projects?.first()?.openAlertCount)
		verify(service).findOpenAlertProjects(any(), eq(3))
	}

	@Test
	fun `Should findOpenAlertProjects return 401 without authentication`() {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/open-alerts", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, NOT_AUTHENTICATED)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should deleteProjectById return 200 with the delete permission`() {
		// Arrange
		whenever(service.deleteProjectById(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Unit>(OK)
		verify(service).deleteProjectById(any(), eq(projectId))
	}

	@Test
	fun `Should deleteProjectById return 403 without the delete permission`() {
		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateProjectById patch changed fields from the body`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)
		whenever(service.updateProjectById(any(), any(), any())).thenReturn(Mono.just(commonProject()))

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
}
