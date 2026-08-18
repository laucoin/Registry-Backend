package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_COLLECTION_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.stream.Stream

/**
 * The v2 `page`/`size` bounds are declared once, on the shared
 * [fr.laucoin.registry.backend.infrastructure.out.api.dto.PageQueryDto] that every
 * paginated v2 endpoint binds, so they are covered once here rather than repeated in
 * each controller's test. Both shapes are exercised: the sorted one (`sort`/`direction`
 * on the subclass) and the plain one, since only the subclass adds fields and a binding
 * regression could affect either. A rejected query must never reach the service.
 */
class PageQueryValidationV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var activityService: IActivityService

	@MockitoBean
	private lateinit var activityReaderMapper: ActivityReaderDtoMapper

	@MockitoBean
	private lateinit var participantService: IParticipantService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val SORTED_URL = "/api/v2/projects/{projectId}/activities"
		private const val PLAIN_URL = "/api/v2/projects/{projectId}/participants/{id}/movements"

		@JvmStatic
		fun `Should reject an out-of-bounds page query`(): Stream<Arguments> = Stream.of(
			Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
			Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
			Arguments.of(null, DEFAULT_COLLECTION_LIMIT + 1, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should reject an out-of-bounds page query`(page: Int?, size: Int?, expectedCode: String) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(SORTED_URL, listOf(projectId), listOf("page" to page, "size" to size)))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)
		verifyNoInteractions(activityService)
	}

	@ParameterizedTest
	@MethodSource("Should reject an out-of-bounds page query")
	fun `Should reject an out-of-bounds page query on an endpoint without sorting`(
		page: Int?,
		size: Int?,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R),
			)
			.get()
			.uri(
				uriBuilder(
					PLAIN_URL,
					listOf(projectId, UUID.randomUUID()),
					listOf("page" to page, "size" to size),
				),
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)
		verifyNoInteractions(participantService)
	}

	@Test
	fun `Should accept the maximum page size`() {
		// Arrange
		val pageable = PageableModel(0, DEFAULT_COLLECTION_LIMIT)
		whenever(activityService.findActivitiesPage(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(pageable, totalElements = 0, emptyList())))
		whenever(activityReaderMapper.toDtoPage(any()))
			.thenReturn(PageModel(pageable, totalElements = 0, emptyList()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					SORTED_URL,
					listOf(projectId),
					listOf("page" to 0, "size" to DEFAULT_COLLECTION_LIMIT),
				),
			)
			.exchange()

		// Assert
		result.expectStatus().isOk
	}
}
