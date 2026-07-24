package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_BEGIN_LATER_THAN_END_TIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_U
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_C
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.COMMUNICATION
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ProjectWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ProjectControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IProjectService

	@MockitoBean
	private lateinit var readerMapper: ProjectReaderDtoMapper

	@MockitoBean
	private lateinit var optionsReaderMapper: ProjectOptionsReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ProjectWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v1/projects"

		@JvmStatic
		fun `Should findProjects return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of(emptyList<String>(), "not locale", null, null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, 0, null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, 200, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, null, "text", null, null, null),
			Arguments.of(listOf(REGISTRY_PROJECT_R), null, null, null, null, false, null, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, true, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, false, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, null, null),
			Arguments.of(emptyList<String>(), null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
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
		pageNumber: Int?,
		pageSize: Int?,
		textSearched: String?,
		withProfile: Boolean?,
		visibilitySearched: Boolean?,
		dateTimeSearched: String?,
	) {
		// Arrange
		val expectedPageNumber = pageNumber ?: 0
		val expectedPageSize = pageSize ?: 20
		val expectedWithProfile = withProfile ?: true
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = ProjectSearchParamModel(
			textSearched = textSearched,
			visibilitySearched = visibilitySearched,
			dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(ProjectModel()))
		whenever(service.findProjectsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ProjectReaderDto())),
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
						Pair("pageNumber", pageNumber),
						Pair("pageSize", pageSize),
						Pair("textSearched", textSearched),
						Pair("withProfile", withProfile),
						Pair("visibilitySearched", visibilitySearched),
						Pair("dateTimeSearched", dateTimeSearched),
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
			expectedWithProfile,
			searchParams
		)
		verify(readerMapper).toDtoPage(any())
		verifyNoInteractions(optionsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findProjects return 403`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					emptyList(),
					listOf(
						Pair("withProfile", false),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(optionsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findProjects throw due to wrong params`(
		pageNumber: Int?,
		pageSize: Int?,
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
						Pair("pageNumber", pageNumber),
						Pair("pageSize", pageSize),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(optionsReaderMapper)
		verifyNoInteractions(writerMapper)
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
		verify(readerMapper).toDto(any())
		verifyNoInteractions(optionsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should getAvailableProjectOptions return 200`() {
		// Arrange
		whenever(service.availableProjectOptions()).thenReturn(Flux.just(COMMUNICATION))
		whenever(optionsReaderMapper.toDto(any())).thenReturn(
			ProjectOptionsReaderDto(
				ACTIVITY,
				"label",
				"question",
				emptyList()
			)
		)

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_METADATA_R)
			.get()
			.uri(uriBuilder("$BASE_URL/options", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).availableProjectOptions()
		verifyNoInteractions(readerMapper)
		verify(optionsReaderMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createProject return 200`() {
		// Arrange
		val project = ProjectWriterDto(
			name = "project",
			begin = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
			end = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
			options = listOf(ACTIVITY)
		)
		whenever(service.createProject(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(writerMapper.toModel(any())).thenReturn(ProjectModel())
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROJECT_C)
			.post()
			.uri(BASE_URL)
			.bodyValue(project)
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)

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
		verifyNoInteractions(optionsReaderMapper)
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
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any())
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.bodyValue(project)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(optionsReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should disableProjectById return 200`() {
		// Arrange
		whenever(service.disableProjectById(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)

		verify(service).disableProjectById(any(), eq(projectId))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableProjectById return 200`() {
		// Arrange
		whenever(service.enableProjectById(any(), any())).thenReturn(Mono.just(ProjectModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectReaderDto>(OK)

		verify(service).enableProjectById(any(), eq(projectId))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(optionsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteProjectById return 200`() {
		// Arrange
		whenever(service.deleteProjectById(any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)

		verify(service).deleteProjectById(eq(projectId))
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(optionsReaderMapper)
		verifyNoInteractions(writerMapper)
	}
}
