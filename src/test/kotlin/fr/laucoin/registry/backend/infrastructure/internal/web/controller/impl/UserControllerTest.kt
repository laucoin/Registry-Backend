package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.UserDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserControllerTest(
    @Autowired private val webClient: WebTestClient,
): TestContext() {
    @MockitoBean
    private lateinit var service: IUserService

    @MockitoSpyBean
    private lateinit var roleService: IRoleService

    @MockitoSpyBean
    private lateinit var mapper: UserDtoMapper

    companion object {
        private const val BASE_URL = "/api/users"

        @JvmStatic
        fun `Should findUsers return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null),
            Arguments.of(50, null, null, null, null),
            Arguments.of(null, 25, null, null, null),
            Arguments.of(null, null, ASC, null, null),
            Arguments.of(null, null, DESC, null, null),
            Arguments.of(null, null, null, true, null),
            Arguments.of(null, null, null, false, null),
            Arguments.of(null, null, null, null, "searched"),
        )

        @JvmStatic
        fun `Should updateUserRole return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of("ROLE_USER"),
            Arguments.of(null),
        )

        @JvmStatic
        fun `Should return 401`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            return Stream.of(
                Arguments.of(GET, BASE_URL, emptyList<String>(), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(uuid), null),
                Arguments.of(GET, "$BASE_URL/search", emptyList<String>(), null),
                Arguments.of(GET, "$BASE_URL/roles", emptyList<String>(), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/role", listOf(uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/block", listOf(uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/unblock", listOf(uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/impersonate", listOf(uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(uuid), null),
            )
        }

        @JvmStatic
        fun `Should return 403`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            return Stream.of(
                Arguments.of(GET, BASE_URL, emptyList<String>(), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(uuid), null),
                Arguments.of(GET, "$BASE_URL/roles", emptyList<String>(), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/role", listOf(uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/block", listOf(uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/unblock", listOf(uuid), null),
                Arguments.of(PATCH, "$BASE_URL/{id}/impersonate", listOf(uuid), null),
                Arguments.of(DELETE, "$BASE_URL/{id}", listOf(uuid), null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should return 401`(
        method: HttpMethod, uri: String, params: List<String>, body: Any?
    ) {
        // Arrange
        val request = webClient
            .method(method)
            .uri(uriBuilder(uri, params, listOf()))

        if (Objects.nonNull(body)) {
            request.bodyValue(body !!)
        }

        // Act
        val result = request.exchange()

        // Assert
        result.expectStatus().isUnauthorized
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should return 403`(
        method: HttpMethod, uri: String, params: List<String>, body: Any?
    ) {
        // Arrange
        val request = webClient
            .authenticate()
            .method(method)
            .uri(uriBuilder(uri, params, listOf()))

        if (Objects.nonNull(body)) {
            request.bodyValue(body !!)
        }

        // Act
        val result = request.exchange()

        // Assert
        result.expectStatus().isForbidden
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
        verifyNoInteractions(roleService)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findUsers return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        searched: String?,
    ) {
        // Arrange
        val expectedOrder = order ?: ASC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 0

        `when`(service.findUsers(any(), any(), anyOrNull())).thenReturn(Flux.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_R")
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    emptyList(),
                    listOf(
                        Pair("offset", offset),
                        Pair("limit", limit),
                        Pair("order", order),
                        Pair("onlyVisible", onlyVisible),
                        Pair("searched", searched),
                    ),
                )
            )
            .exchange()

        // Assert
        val body = result.body<PageModel<*>>(OK)

        assertNotNull(body)
        body !!.assertPage(
            expectedTotalElements = expectedSize,
            expectedOffset = expectedOffset,
            expectedLimit = expectedLimit,
        )

        verifyNoInteractions(mapper)
        verify(service, times(1)).findUsers(
            order = expectedOrder,
            onlyVisible = expectedOnlyVisible,
            searched = searched,
        )
        verifyNoInteractions(roleService)
    }

    @Test
    fun `Should findUserById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findUserById(any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_R")
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<UserModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).findUserById(uuid, onlyVisible = false)
        verifyNoInteractions(roleService)
    }

    @Test
    fun `Should getAssignableUserRoles return 200`() {
        // Arrange
        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_METADATA_R")
            .get()
            .uri(uriBuilder("$BASE_URL/roles", emptyList(), emptyList()))
            .exchange()

        // Assert
        result.body<List<*>>(OK)
        verifyNoInteractions(mapper)
        verifyNoInteractions(service)
        verify(roleService, times(1)).getAssignableUserRoles(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should updateUserRole return 200`(
        role: String?
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val queryParams = if (Objects.nonNull(role)) listOf(Pair("role", role)) else emptyList()
        `when`(service.updateUserRoleById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_U")
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/role", listOf(uuid), queryParams))
            .exchange()

        // Assert
        result.body<UserModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).updateUserRoleById(any(), eq(uuid), eq(role))
    }

    @Test
    fun `Should blockUserById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.blockUserById(any(), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_U")
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/block", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<UserModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).blockUserById(any(), eq(uuid))
        verifyNoInteractions(roleService)
    }

    @Test
    fun `Should unblockUserById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.unblockUserById(any(), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_U")
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/unblock", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<UserModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).unblockUserById(any(), eq(uuid))
        verifyNoInteractions(roleService)
    }

    @Test
    fun `Should impersonateUserById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.impersonateUserById(any(), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_U")
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/impersonate", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<UserModel>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).impersonateUserById(any(), eq(uuid))
        verifyNoInteractions(roleService)
    }

    @Test
    fun `Should deleteUserById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.deleteUserById(any(), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate("REGISTRY_USER_D")
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verifyNoInteractions(mapper)
        verify(service, times(1)).deleteUserById(any(), eq(uuid))
        verifyNoInteractions(roleService)
    }
}
