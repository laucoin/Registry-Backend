package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROFILE_C
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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

class UserProjectProfileV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IUserProjectProfileService

	@MockitoBean
	private lateinit var readerMapper: ProjectProfileReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/users/profiles"
	}

	@Test
	fun `Should findUserProjectProfiles call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))
		whenever(service.findProjectProfilesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findProjectProfilesPage(
			currentUser().id!!,
			pageable,
			ProjectProfileSearchParamModel(textSearched = null, availabilitySearched = null, statusSearched = null, dateTimeSearched = null),
			emptyList(),
		)
		verify(readerMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should findUserProjectProfiles thread favorite query param to search params`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ProjectProfileModel()))
		whenever(service.findProjectProfilesPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("favorite", true))))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findProjectProfilesPage(
			currentUser().id!!,
			pageable,
			ProjectProfileSearchParamModel(textSearched = null, availabilitySearched = null, statusSearched = null, dateTimeSearched = null, favoriteSearched = true),
			emptyList(),
		)
	}

	@Test
	fun `Should findUserProjectProfiles return 400 when a query param is invalid`() {
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
	fun `Should findUserProjectProfiles return 400 on an unknown sort field`() {
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
	fun `Should acceptUserProjectProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserProjectProfileStatusById(any(), any(), any())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/accept", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).updateUserProjectProfileStatusById(any(), eq(uuid), eq(ACCEPTED))
	}

	@Test
	fun `Should rejectUserProjectProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserProjectProfileStatusById(any(), any(), any())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/reject", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).updateUserProjectProfileStatusById(any(), eq(uuid), eq(REJECTED))
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

	@Test
	fun `Should toggleFavoriteUserProjectProfileById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.toggleFavoriteProjectProfileById(any(), any())).thenReturn(Mono.just(ProjectProfileModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).toggleFavoriteProjectProfileById(any(), eq(uuid))
	}

	@Test
	fun `Should deleteUserProfileById return 204`() {
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
		result.body<Void>(NO_CONTENT)
		verify(service).deleteUserProjectProfileById(any(), eq(uuid))
	}
}
