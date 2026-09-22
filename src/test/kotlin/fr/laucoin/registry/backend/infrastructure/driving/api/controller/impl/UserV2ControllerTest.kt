package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_D
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_METADATA_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_U
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.UserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.UserRoleReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IUserService

	@MockitoBean
	private lateinit var readerMapper: UserReaderDtoMapper

	@MockitoBean
	private lateinit var userRoleReaderMapper: UserRoleReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/users"
	}

	@Test
	fun `Should findUsers call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(UserModel()))
		whenever(service.findUsersPage(any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findUsersPage(pageable, UserSearchParamModel(textSearched = null, visibilitySearched = null), emptyList())
		verify(readerMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
	}

	@Test
	fun `Should findUsers return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findUsers return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findUserById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.findUserById(any(), anyOrNull())).thenReturn(Mono.just(UserModel()))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).findUserById(uuid, visibilitySearched = null)
	}

	@Test
	fun `Should getAssignableUserRoles return 200`() {
		// Arrange
		whenever(service.assignableUserRoles(any())).thenReturn(Flux.just("USER_ROLE"))
		whenever(userRoleReaderMapper.toDto(any())).thenReturn(LabelDto("value", "label"))

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_METADATA_R)
			.get()
			.uri(uriBuilder("$BASE_URL/roles", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).assignableUserRoles(any())
		verifyNoInteractions(readerMapper)
		verify(userRoleReaderMapper).toDto(any())
	}

	@Test
	fun `Should updateUserRole return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val role = "ROLE_USER"
		whenever(service.updateUserRoleById(any(), any(), anyOrNull())).thenReturn(Mono.just(UserModel()))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/role", listOf(uuid), listOf(Pair("role", role))))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).updateUserRoleById(any(), eq(uuid), eq(role))
	}

	@Test
	fun `Should blockUserById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.blockUserById(any(), eq(uuid))).thenReturn(Mono.just(UserModel()))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/block", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).blockUserById(any(), eq(uuid))
	}

	@Test
	fun `Should unblockUserById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.unblockUserById(any(), eq(uuid))).thenReturn(Mono.just(UserModel()))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).unblockUserById(any(), eq(uuid))
	}

	@Test
	fun `Should impersonateUserById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.impersonateUserById(any(), eq(uuid))).thenReturn(Mono.just(UserModel()))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_D)
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/impersonate", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).impersonateUserById(any(), eq(uuid))
	}

	@Test
	fun `Should impersonateCurrentUser use POST and return 200`() {
		// Arrange
		whenever(service.impersonateUserById(any(), any())).thenReturn(Mono.just(UserModel()))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/impersonate", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).impersonateUserById(any(), any())
	}

	@Test
	fun `Should deleteUserById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.deleteUserById(any(), eq(uuid))).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_D)
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).deleteUserById(any(), eq(uuid))
	}
}
