package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
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
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.UserRoleWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.UserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.UserRoleReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
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
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserControllerTest: TestContext() {
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

		@JvmStatic
		fun `Should findUsers return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null),
			Arguments.of(null, 0, null, null, null),
			Arguments.of(null, null, 200, null, null),
			Arguments.of(null, null, null, null, null),
			Arguments.of(null, null, null, "text", null),
			Arguments.of(null, null, null, null, true),
		)

		@JvmStatic
		fun `Should findUsers throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Should updateUserRole return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("ROLE_USER"),
			Arguments.of(null),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findUsers return 200`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = UserSearchParamModel(
			query = query,
			isVisible = isVisible,
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(UserModel()))
		whenever(service.findUsersPage(any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(UserReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
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
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findUsersPage(pageable, searchParams)
		verify(readerMapper).toDtoPage(any())
		verifyNoInteractions(userRoleReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findUsers throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
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

		verifyNoInteractions(service)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(userRoleReaderMapper)
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
		verify(service).findUserById(uuid, isVisible = null)
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

	@ParameterizedTest
	@MethodSource
	fun `Should updateUserRole return 200`(
		role: String?
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.updateUserRoleById(any(), any(), anyOrNull())).thenReturn(Mono.just(UserModel()))
		whenever(readerMapper.toDto(any())).thenReturn(UserReaderDto())

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/role", listOf(uuid), emptyList()))
			.bodyValue(UserRoleWriterDto(role))
			.exchange()

		// Assert
		result.body<UserReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).updateUserRoleById(any(), eq(uuid), eq(role))
	}

	@Test
	fun `Should blockUserById return 200`() {
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
	fun `Should unblockUserById return 200`() {
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
	fun `Should deleteCurrentUser return 204`() {
		// Arrange
		whenever(service.deleteCurrentUser(any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate()
			.delete()
			.uri(uriBuilder("$BASE_URL/current", emptyList(), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(userRoleReaderMapper)
		verify(service).deleteCurrentUser(any())
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

	@Test
	fun `Should findUsers forward the role filter to the service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(UserModel()))
		whenever(service.findUsersPage(any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(UserReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("role", "ROLE_USER"))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findUsersPage(pageable, UserSearchParamModel(query = null, role = "ROLE_USER"))
	}

	@Test
	fun `Should findUsers refuse a role filter over 50 characters`() {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("role", "R".repeat(51)))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateUserRole refuse a role over 50 characters`() {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/role", listOf(UUID.randomUUID()), emptyList()))
			.bodyValue(UserRoleWriterDto("R".repeat(51)))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findUsers refuse a role filter with forbidden characters`() {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_R)
			.get()
			.uri(uriBuilder(BASE_URL, emptyList(), listOf(Pair("role", "ROLE';--"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_FORMAT_INVALID)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateUserRole refuse a role with forbidden characters`() {
		// Act
		val result = webClient
			.authenticate(REGISTRY_USER_U)
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/role", listOf(UUID.randomUUID()), emptyList()))
			.bodyValue(UserRoleWriterDto("ROLE';--"))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_FORMAT_INVALID)
		verifyNoInteractions(service)
	}
}
