package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROFILE_C
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream

class UserProjectProfileControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IUserProjectProfileService

	@MockitoBean
	private lateinit var readerMapper: ProjectProfileReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v1/users/profiles"

		@JvmStatic
		fun `Should findUserProjectProfiles return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null, null, null),
			Arguments.of(null, 0, null, null, null, null, null),
			Arguments.of(null, null, 200, null, null, null, null),
			Arguments.of(null, null, null, null, null, null, null),
			Arguments.of(null, null, null, "text", null, null, null),
			Arguments.of(null, null, null, null, true, null, null),
			Arguments.of(null, null, null, null, null, BLOCKED, null),
			Arguments.of(null, null, null, null, null, INVITED, null),
			Arguments.of(null, null, null, null, null, REJECTED, null),
			Arguments.of(null, null, null, null, null, ACCEPTED, null),
			Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should manageUserProjectProfileAcceptance return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of(true),
			Arguments.of(false)
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findUserProjectProfiles return 200`(
		requestedLocale: String?,
		pageNumber: Int?,
		pageSize: Int?,
		textSearched: String?,
		availabilitySearched: Boolean?,
		statusSearched: ProfileStatusEnum?,
		dateTimeSearched: String?,
	) {
		// Arrange
		val expectedPageNumber = pageNumber ?: 0
		val expectedPageSize = pageSize ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = ProjectProfileSearchParamModel(
			textSearched = textSearched,
			availabilitySearched = availabilitySearched,
			statusSearched = statusSearched,
			dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))
		whenever(service.findProjectProfilesPage(any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ProjectProfileReaderDto())),
		)

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
						Pair("textSearched", textSearched),
						Pair("availabilitySearched", availabilitySearched),
						Pair("statusSearched", statusSearched),
						Pair("dateTimeSearched", dateTimeSearched),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findProjectProfilesPage(currentUser().id!!, pageable, searchParams)
		verify(readerMapper).toDtoPage(page)
	}

	@Test
	fun `Should createSupportProjectProfile return 200`() {
		// Arrange
		whenever(service.createSupportProjectProfile(any(), any())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROFILE_C)
			.post()
			.uri(uriBuilder("$BASE_URL/{projectId}/support", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(readerMapper).toDto(any())
		verify(service).createSupportProjectProfile(any(), eq(projectId))
	}

	@ParameterizedTest
	@MethodSource
	fun `Should manageUserProjectProfileAcceptance return 200`(accepted: Boolean) {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserProjectProfileStatusById(any(), any(), any())).thenReturn(
			Mono.just(
				ProjectProfileModel()
			)
		)
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/accept/{accepted}", listOf(uuid, accepted), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verify(service).updateUserProjectProfileStatusById(any(), eq(uuid), eq(if (accepted) ACCEPTED else REJECTED))
	}

	@Test
	fun `Should deleteUserProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteUserProjectProfileById(any(), eq(uuid))).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)

		verify(service).deleteUserProjectProfileById(any(), eq(uuid))
	}
}
