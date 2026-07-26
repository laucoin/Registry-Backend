package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_PROFILE_C
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.test.ModelExt.commonProjectProfile
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals

class UserProjectProfileV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IUserProjectProfileService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/users/profiles"
	}

	private fun profilePage(): Mono<PageModel<ProjectProfileModel>> =
		Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonProjectProfile())))

	@Test
	fun `Should findUserProjectProfiles return 200 with the v2 list grammar`() {
		// Arrange
		whenever(service.findProjectProfilesPage(any(), any(), any())).thenReturn(profilePage())

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
						Pair("q", "john"),
						Pair("available", true),
						Pair("status", ACCEPTED),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<ProjectProfileReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val searchParamsCaptor = argumentCaptor<ProjectProfileSearchParamModel>()
		verify(service).findProjectProfilesPage(any(), pageableCaptor.capture(), searchParamsCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals("john", searchParamsCaptor.firstValue.textSearched)
		assertEquals(true, searchParamsCaptor.firstValue.availabilitySearched)
		assertEquals(listOf(ACCEPTED), searchParamsCaptor.firstValue.statusSearched)
	}

	@Test
	fun `Should findSentInvitations return 200 for the authenticated caller`() {
		// Arrange
		whenever(service.findSentInvitationsPage(any(), any(), any())).thenReturn(profilePage())

		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/sent", emptyList(), listOf(Pair("page", 1), Pair("size", 10))))
			.exchange()

		// Assert
		result.body<PageModel<ProjectProfileReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		verify(service).findSentInvitationsPage(any(), pageableCaptor.capture(), any())
		assertEquals(PageableModel(offset = 10, limit = 10), pageableCaptor.firstValue)
	}

	@Test
	fun `Should findSentInvitations return 401 without authentication`() {
		// Act
		val result = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/sent", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, NOT_AUTHENTICATED)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should toggleFavoriteUserProjectProfileById return 200 for the authenticated caller`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.toggleFavorite(any(), any())).thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).toggleFavorite(any(), eq(id))
	}

	@Test
	fun `Should toggleFavoriteUserProjectProfileById return 401 without authentication`() {
		// Act
		val result = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/favorite", listOf(UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, NOT_AUTHENTICATED)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should acceptUserProjectProfileById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateUserProjectProfileStatusById(any(), any(), any()))
			.thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/accept", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).updateUserProjectProfileStatusById(any(), eq(id), eq(ACCEPTED))
	}

	@Test
	fun `Should rejectUserProjectProfileById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateUserProjectProfileStatusById(any(), any(), any()))
			.thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/reject", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).updateUserProjectProfileStatusById(any(), eq(id), eq(REJECTED))
	}

	@Test
	fun `Should createSupportProjectProfile return 200 with the create authority`() {
		// Arrange
		whenever(service.createSupportProjectProfile(any(), any())).thenReturn(Mono.just(commonProjectProfile()))

		// Act
		val result = webClient
			.authenticate(REGISTRY_PROFILE_C)
			.post()
			.uri(uriBuilder("$BASE_URL/{projectId}/support", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<ProjectProfileReaderDto>(OK)
		verify(service).createSupportProjectProfile(any(), eq(projectId))
	}

	@Test
	fun `Should createSupportProjectProfile return 403 without the create authority`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{projectId}/support", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteUserProfileById return 200`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteUserProjectProfileById(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<Unit>(OK)
		verify(service).deleteUserProjectProfileById(any(), eq(id))
	}
}
