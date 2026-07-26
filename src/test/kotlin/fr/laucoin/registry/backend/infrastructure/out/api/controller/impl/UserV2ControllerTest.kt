package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_D
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.EMAIL
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_NAME
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.UserWriterDto
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
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
import java.util.UUID
import kotlin.test.assertEquals

class UserV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IUserService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/users"
	}

	private fun userPage(): Mono<PageModel<UserModel>> =
		Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonUser())))

	@Test
	fun `Should findUsers return 200 with the v2 list grammar`() {
		// Arrange
		whenever(service.findUsersPage(any(), any(), any())).thenReturn(userPage())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					emptyList(),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("q", "john"),
						Pair("visible", true),
						Pair("sort", "lastName,-email"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<UserReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<UserSortFieldEnum>>>()
		verify(service).findUsersPage(pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(LAST_NAME), SortModel(EMAIL, descending = true)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findUsers reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("sort", "passwordHash"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findUsers return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should blockUserById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.blockUserById(any(), any())).thenReturn(Mono.just(commonUser()))

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/block", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)
		verify(service).blockUserById(any(), eq(id))
	}

	@Test
	fun `Should unblockUserById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.unblockUserById(any(), any())).thenReturn(Mono.just(commonUser()))

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)
		verify(service).unblockUserById(any(), eq(id))
	}

	@Test
	fun `Should findUserById return 200 with the read authority`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findUserById(any(), anyOrNull())).thenReturn(Mono.just(commonUser()))

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)
		verify(service).findUserById(id, visibilitySearched = null)
	}

	@Test
	fun `Should findUserById return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should getAssignableUserRoles return 200 with the metadata read authority`() {
		// Arrange
		whenever(service.assignableUserRoles(any())).thenReturn(Flux.just("USER"))

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_METADATA_R)
			.get()
			.uri(uriBuilder("$BASE_URL/assignable-roles", emptyList(), emptyList()))
			.exchange()

		// Assert
		val roles = result.body<List<LabelDto>>(OK)
		assertEquals("USER", roles?.first()?.value)
		verify(service).assignableUserRoles(any())
	}

	@Test
	fun `Should getAssignableUserRoles return 403 without the metadata read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/assignable-roles", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should deleteCurrentUser return 200 for the authenticated caller`() {
		// Arrange
		whenever(service.deleteCurrentUser(any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/me", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<Unit>(OK)
		verify(service).deleteCurrentUser(any())
	}

	@Test
	fun `Should deleteCurrentUser return 401 without authentication`() {
		// Act
		val result = webClient
			.delete()
			.uri(uriBuilder("$BASE_URL/me", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.assertError(UNAUTHORIZED, NOT_AUTHENTICATED)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should deleteUserById return 200 with the delete authority`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteUserById(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_D)
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(id), emptyList()))
			.exchange()

		// Assert
		result.body<Unit>(OK)
		verify(service).deleteUserById(any(), eq(id))
	}

	@Test
	fun `Should deleteUserById return 403 without the delete authority`() {
		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateUser patch changed fields from the body`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateUserRoleById(any(), any(), any())).thenReturn(Mono.just(commonUser()))

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(id), emptyList()))
			.bodyValue(UserWriterDto(role = "ROLE_2"))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)
		verify(service).updateUserRoleById(any(), eq(id), eq("ROLE_2"))
	}
}
