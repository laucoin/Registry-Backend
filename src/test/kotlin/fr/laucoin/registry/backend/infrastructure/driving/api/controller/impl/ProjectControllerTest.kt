package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.COMMUNICATION
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectWithProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R as PROJECT_SCOPED_PROJECT_R

class ProjectControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IProjectService

	@MockitoBean
	private lateinit var readerMapper: ProjectWithProfileReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ProjectWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects"

		@JvmStatic
		fun `Should findProjects return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of(emptyList<String>(), "not locale", null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, 0, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, 200, null, null, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, null, "text", null, null),
			Arguments.of(listOf(REGISTRY_PROJECT_R), null, null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, null, null, true, null),
			Arguments.of(emptyList<String>(), null, null, null, null, false, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should findProjects throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Wrong ProjectDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				ProjectWriterDto(
					name = "",
					begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					options = emptyList()
				),
				PROJECT_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ProjectWriterDto(
					name = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
					begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					options = emptyList()
				),
				PROJECT_NAME_TOO_LONG,
			),
			Arguments.of(
				ProjectWriterDto(
					name = "project",
					begin = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					end = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					options = emptyList()
				),
				PROJECT_BEGIN_LATER_THAN_END_TIME,
			),
			Arguments.of(
				ProjectWriterDto(
					name = "project",
					begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					options = listOf(COMMUNICATION)
				),
				PROJECT_OPTIONS_MISSING,
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findProjects return 200`(
		authorities: List<String>,
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		at: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = ProjectSearchParamModel(
			query = query,
			isVisible = isVisible,
			at = at?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ProjectModel()))
		whenever(service.findProjectsPage(any(), any(), anyOrNull(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ProjectWithProfileReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*authorities.toTypedArray())
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					emptyList(),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("at", at),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findProjectsPage(
			currentUser(*authorities.toTypedArray()),
			pageable,
			null,
			searchParams
		)
		verify(readerMapper).toDtoPage(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findProjects pass the favorite filter and accept the favorite sort`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val captor = argumentCaptor<ProjectSearchParamModel>()
		whenever(service.findProjectsPage(any(), any(), anyOrNull(), any()))
			.thenReturn(Mono.just(PageModel(pageable, totalElements = 0, emptyList())))
		whenever(readerMapper.toDtoPage(any())).thenReturn(PageModel(pageable, totalElements = 0, emptyList()))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("isFavorite", true), Pair("sort", "-favorite"))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findProjectsPage(any(), any(), anyOrNull(), captor.capture())
		assertEquals(true, captor.firstValue.isFavorite)
		assertEquals(SortModel("favorite", isDescending = true), captor.firstValue.sort)
	}

	@Test
	fun `Should findProjects throw due to an invalid favorite filter`() {
		// Arrange
		val invalidValue = "abc"

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("isFavorite", invalidValue))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TYPE_MISMATCH)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should findProjects throw due to an unknown sort field`() {
		// Arrange
		val unknownField = "unknown"

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("sort", unknownField))))
			.exchange()

		// Assert
		result.expectStatus().isBadRequest

		verifyNoInteractions(service)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findProjects throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					emptyList(),
					listOf(
						Pair("page", page),
						Pair("size", size),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findProjectById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.findProjectWithProfileById(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectWithProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectWithProfileReaderDto>(OK)

		verify(service).findProjectWithProfileById(currentUser(REGISTRY_PROJECT_R), uuid)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createProject refuse more than 20 options`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = List(21) { ACTIVITY },
		)

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_C)
			.post()
			.uri(BASE_URL)
			.bodyValue(project)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, LIST_TOO_LARGE)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createProject return 201`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)
		whenever(service.createProject(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(writerMapper.toModel(any())).thenReturn(ProjectModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectWithProfileReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_C)
			.post()
			.uri(BASE_URL)
			.bodyValue(project)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<ProjectWithProfileReaderDto>(CREATED)

		verify(service).createProject(any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any())
	}

	@ParameterizedTest
	@MethodSource("Wrong ProjectDto")
	fun `Should createProject return 400`(
		project: ProjectWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(BASE_URL)
			.bodyValue(project)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateProjectById return 200`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)

		whenever(service.updateProjectById(any(), any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(writerMapper.toModel(any())).thenReturn(ProjectModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectWithProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId), emptyList()))
			.bodyValue(project)
			.exchange()

		// Assert
		result.body<ProjectWithProfileReaderDto>(OK)

		verify(service).updateProjectById(any(), eq(defaultProjectId), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any())
	}

	@Test
	fun `Should updateProjectById accept the matching If-Match`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)

		whenever(service.updateProjectById(any(), any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(writerMapper.toModel(any())).thenReturn(ProjectModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectWithProfileReaderDto())
		whenever(service.findProjectById(any(), anyOrNull())).thenReturn(
			Mono.just(ProjectModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(project)
			.exchange()

		// Assert
		result.body<ProjectWithProfileReaderDto>(OK)

		verify(service).updateProjectById(any(), eq(defaultProjectId), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any())
	}

	@Test
	fun `Should updateProjectById throw when the element changed since it was read`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)

		whenever(writerMapper.toModel(any())).thenReturn(ProjectModel())
		whenever(service.findProjectById(any(), anyOrNull())).thenReturn(
			Mono.just(ProjectModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(project)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateProjectById(any(), eq(defaultProjectId), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong ProjectDto")
	fun `Should updateProjectById return 400`(
		project: ProjectWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId), emptyList()))
			.bodyValue(project)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should disableProjectById return 200`() {
		// Arrange
		whenever(service.disableProjectById(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectWithProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectWithProfileReaderDto>(OK)

		verify(service).disableProjectById(any(), eq(defaultProjectId))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableProjectById return 200`() {
		// Arrange
		whenever(service.enableProjectById(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectWithProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectWithProfileReaderDto>(OK)

		verify(service).enableProjectById(any(), eq(defaultProjectId))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should addFavoriteProject return 204 with the global read authority`() {
		// Arrange
		whenever(service.addFavoriteProject(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verify(service).addFavoriteProject(currentUser(REGISTRY_PROJECT_R), defaultProjectId)
	}

	@Test
	fun `Should addFavoriteProject return 204 with the project read permission`() {
		// Arrange
		whenever(service.addFavoriteProject(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate(buildAuthority(PROJECT_SCOPED_PROJECT_R))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verify(service).addFavoriteProject(any(), eq(defaultProjectId))
	}

	@Test
	fun `Should addFavoriteProject return 403 without read access`() {
		// Arrange
		val authority = buildAuthority(REGISTRY_PROJECT_D)

		// Act
		val result = webClient
			.authenticate(authority)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should removeFavoriteProject return 204 with the project read permission`() {
		// Arrange
		whenever(service.removeFavoriteProject(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate(buildAuthority(PROJECT_SCOPED_PROJECT_R))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verify(service).removeFavoriteProject(any(), eq(defaultProjectId))
	}

	@Test
	fun `Should removeFavoriteProject return 403 without read access`() {
		// Arrange
		val authority = buildAuthority(REGISTRY_PROJECT_D)

		// Act
		val result = webClient
			.authenticate(authority)
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should removeFavoriteProject return 204 with the global read authority`() {
		// Arrange
		whenever(service.removeFavoriteProject(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verify(service).removeFavoriteProject(currentUser(REGISTRY_PROJECT_R), defaultProjectId)
	}

	@ParameterizedTest
	@ValueSource(strings = ["POST", "DELETE"])
	fun `Should favorite endpoints return 403 with the read permission of another project`(method: String) {
		// Arrange
		val otherProjectAuthority = "${UUID.randomUUID()}_$PROJECT_SCOPED_PROJECT_R"

		// Act
		val result = webClient
			.authenticate(otherProjectAuthority)
			.method(HttpMethod.valueOf(method))
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
	}

	@ParameterizedTest
	@ValueSource(strings = ["POST", "DELETE"])
	fun `Should favorite endpoints return 400 with an invalid project id`(method: String) {
		// Arrange
		val invalidId = "not uuid"

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.method(HttpMethod.valueOf(method))
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(invalidId), emptyList()))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TYPE_MISMATCH)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should addFavoriteProject return 404 when the service cannot find the project`() {
		// Arrange
		val notFound =
			RegistryException(NOT_FOUND, NOT_FOUND_WITH_GIVEN_IDENTIFIER, arrayListOf(defaultProjectId.toString()))
		whenever(service.addFavoriteProject(any(), any())).thenReturn(Mono.error(notFound))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(NOT_FOUND, NOT_FOUND_WITH_GIVEN_IDENTIFIER)
	}

	@Test
	fun `Should findProjectById expose the favorite flag in the response body`() {
		// Arrange
		whenever(service.findProjectWithProfileById(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectWithProfileReaderDto(favorite = true))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_R)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		assertEquals(true, result.body<Map<*, *>>(OK)?.get("favorite"))
	}

	@Test
	fun `Should deleteProjectById return 204`() {
		// Arrange
		whenever(service.deleteProjectById(any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verify(service).deleteProjectById(eq(defaultProjectId))
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
	}
}
