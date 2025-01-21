package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_STATUS_NOT_ACCEPTED_OR_REJECTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.test.ModelExt.assertPage
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
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
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserEventProfileControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IUserEventProfileService

    companion object {
        private const val BASE_URL = "/api/users/profiles"

        @JvmStatic
        fun `Should findUserEventProfiles return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null, null, null, null, null, null, null, null),
            Arguments.of(50, null, null, null, null, null, null, null, null),
            Arguments.of(null, 25, null, null, null, null, null, null, null),
            Arguments.of(null, null, ASC, null, null, null, null, null, null),
            Arguments.of(null, null, DESC, null, null, null, null, null, null),
            Arguments.of(null, null, null, true, null, null, null, null, null),
            Arguments.of(null, null, null, false, null, null, null, null, null),
            Arguments.of(null, null, null, null, null, INVITED, null, null, null),
            Arguments.of(null, null, null, null, null, ACCEPTED, null, null, null),
            Arguments.of(null, null, null, null, null, REJECTED, null, null, null),
            Arguments.of(null, null, null, null, true, null, null, null, null),
            Arguments.of(null, null, null, null, false, null, null, null, null),
            Arguments.of(null, null, null, null, null, null, "searched", null, null),
            Arguments.of(null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
            Arguments.of(null, null, null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
        )

        @JvmStatic
        fun `Should acceptEventProfile return 200`(): Stream<Arguments> = Stream.of(
            Arguments.of(ACCEPTED),
            Arguments.of(REJECTED)
        )

        @JvmStatic
        fun `Should return 401`(): Stream<Arguments> {
            val uuid = UUID.randomUUID()
            return Stream.of(
                Arguments.of(GET, BASE_URL, emptyList<String>(), null),
                Arguments.of(GET, "$BASE_URL/{id}", listOf(uuid), null),
                Arguments.of(POST, "$BASE_URL/{id}/status/{status}", listOf(uuid, ACCEPTED), null),
                Arguments.of(POST, "$BASE_URL/{id}/status/{status}", listOf(uuid, REJECTED), null),
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
        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findUserEventProfiles return 200`(
        offset: Int?,
        limit: Int?,
        order: Direction?,
        onlyVisible: Boolean?,
        onlyUsable: Boolean?,
        status: ProfileStatusEnum?,
        searched: String?,
        startAccess: String?,
        endAccess: String?,
    ) {
        // Arrange
        val expectedOrder = order ?: ASC
        val expectedOnlyVisible = onlyVisible ?: true
        val expectedOnlyUsable = onlyUsable ?: true
        val expectedOffset = offset ?: 0
        val expectedLimit = limit ?: 20
        val expectedSize = 0

        `when`(
            service.findUserEventProfiles(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.empty())

        // Act
        val result = webClient
            .authenticate()
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
                        Pair("onlyUsable", onlyUsable),
                        Pair("status", status),
                        Pair("searched", searched),
                        Pair("startAccess", startAccess),
                        Pair("endAccess", endAccess),
                    ),
                )
            )
            .exchange()

        // Assert
        val body = result.body<PageDto<*>>(OK)

        assertNotNull(body)
        body !!.assertPage(
            expectedTotalElements = expectedSize,
            expectedOffset = expectedOffset,
            expectedLimit = expectedLimit,
        )

        verify(service, times(1)).findUserEventProfiles(
            userId = any(),
            order = eq(expectedOrder),
            onlyVisible = eq(expectedOnlyVisible),
            onlyUsable = eq(expectedOnlyUsable),
            status = eq(status),
            searched = eq(searched),
            startAccess = anyOrNull(),
            endAccess = anyOrNull(),
        )
    }

    @Test
    fun `Should findUserEventProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.findUserEventProfileById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate()
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileModel>(OK)
        verify(service, times(1)).findUserEventProfileById(any(), eq(uuid), onlyVisible = eq(false))
    }

    @ParameterizedTest
    @MethodSource
    fun `Should acceptEventProfile return 200`(status: ProfileStatusEnum) {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(service.updateUserEventProfileStatusById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder("$BASE_URL/{id}/status/{status}", listOf(uuid, status), emptyList()))
            .exchange()

        // Assert
        result.body<EventProfileModel>(OK)
        verify(service, times(1)).updateUserEventProfileStatusById(any(), eq(uuid), eq(status))
    }

    @Test
    fun `Should acceptEventProfile return 400`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder("$BASE_URL/{id}/status/{status}", listOf(uuid, INVITED), emptyList()))
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, EVENT_PROFILE_STATUS_NOT_ACCEPTED_OR_REJECTED, EVENT_PROFILE_STATUS_NOT_ACCEPTED_OR_REJECTED)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should deleteUserProfileById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        `when`(service.deleteUserEventProfileById(any(), eq(uuid))).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate()
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)
        verify(service, times(1)).deleteUserEventProfileById(any(), eq(uuid))
    }
}
