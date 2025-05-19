package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_MAX_IS_HIGHER_THAN_MIN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DESCRIPTION_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.NumericRangeWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ActivityWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Objects
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono

class ActivityControllerTest(@Autowired private val webClient: WebTestClient): TestContext() {
    @MockitoBean
    private lateinit var service: IActivityService

    @MockitoBean
    private lateinit var readerMapper: ActivityReaderDtoMapper

    @MockitoBean
    private lateinit var movementReaderMapper: MovementReaderDtoMapper

    @MockitoBean
    private lateinit var writerMapper: ActivityWriterDtoMapper

    companion object {
        private const val BASE_URL = "/api/projects/{projectId}/activities"
        private val locale = Locale.ENGLISH

        @JvmStatic
        fun `Should findActivities prepare param, call service and finally cast the result`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("not locale", null, null, null, null, null, null),
                Arguments.of(null, 0, null, null, null, null, null),
                Arguments.of(null, null, 200, null, null, null, null),
                Arguments.of(null, null, null, null, null, null, null),
                Arguments.of(null, null, null, "text", null, null, null),
                Arguments.of(null, null, null, null, true, null, null),
                Arguments.of(null, null, null, null, null, true, null),
                Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
            )
        }

        @JvmStatic
        fun `Should findActivities throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("not uuid", null, null, null, null, null, null, PARAMETER_TYPE_MISMATCH),
                Arguments.of(projectId.toString(), - 1, null, null, null, null, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(projectId.toString(), null, 0, null, null, null, null, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(projectId.toString(), null, 201, null, null, null, null, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
                Arguments.of(projectId.toString(), null, null, null, "not boolean", null, null, PARAMETER_TYPE_MISMATCH),
                Arguments.of(projectId.toString(), null, null, null, null, "not boolean", null, PARAMETER_TYPE_MISMATCH),
                Arguments.of(projectId.toString(), null, null, null, null, null, "2024/11/14 18:34:33", PARAMETER_TYPE_MISMATCH),
            )
        }

        @JvmStatic
        fun `Should findActivityMovements prepare param, call service and finally cast the result`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, null, null, null, null, null),
                Arguments.of(0, null, null, null, null, null),
                Arguments.of(null, 200, null, null, null, null),
                Arguments.of(null, null, null, null, null, null),
                Arguments.of(null, null, true, null, null, null),
                Arguments.of(null, null, null, IN, null, null),
                Arguments.of(null, null, null, null, "2024-11-14T18:34:33.000Z", null),
                Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
            )
        }

        @JvmStatic
        fun `Should findActivityMovements throw due to wrong params`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(- 1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
                Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
                Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
            )
        }

        @JvmStatic
        fun `Wrong ActivityDto`(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ActivityWriterDto(
                    name = null,
                    description = "This is an activity very interesting",
                    duration = "PT15M",
                    allowedParticipants = NumericRangeWriterDto(lower = 1, upper = 10),
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                ),
                ACTIVITY_NAME_NULL_OR_BLANK
            ),
            Arguments.of(
                ActivityWriterDto(
                    name = "",
                    description = "This is an activity very interesting",
                    duration = "PT15M",
                    allowedParticipants = NumericRangeWriterDto(lower = 1, upper = 10),
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                ),
                ACTIVITY_NAME_NULL_OR_BLANK
            ),
            Arguments.of(
                ActivityWriterDto(
                    name = "Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1Activity 1",
                    description = "This is an activity very interesting",
                    duration = "PT15M",
                    allowedParticipants = NumericRangeWriterDto(lower = 1, upper = 10),
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                ),
                ACTIVITY_NAME_TOO_LONG
            ),
            Arguments.of(
                ActivityWriterDto(
                    name = "Activity 1",
                    description = "This is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interestingThis is an activity very interesting",
                    duration = "PT15M",
                    allowedParticipants = NumericRangeWriterDto(lower = 1, upper = 10),
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                ),
                ACTIVITY_DESCRIPTION_TOO_LONG
            ),
            Arguments.of(
                ActivityWriterDto(
                    name = "Activity 1",
                    description = "This is an activity very interesting",
                    duration = "PT15M",
                    allowedParticipants = NumericRangeWriterDto(lower = 0, upper = 10),
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                ),
                ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW
            ),
            Arguments.of(
                ActivityWriterDto(
                    name = "Activity 1",
                    description = "This is an activity very interesting",
                    duration = "PT15M",
                    allowedParticipants = NumericRangeWriterDto(lower = 10, upper = 1),
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                ),
                ACTIVITY_ALLOWED_PARTICIPANTS_MAX_IS_HIGHER_THAN_MIN
            ),
            Arguments.of(
                ActivityWriterDto(
                    name = "Activity 1",
                    description = "This is an activity very interesting",
                    duration = "PT15M",
                    allowedParticipants = NumericRangeWriterDto(lower = 1, upper = 10),
                    endAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
                    startAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
                ),
                ACTIVITY_START_LATER_THAN_END
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findActivities prepare param, call service and finally cast the result`(
        requestedLocale: String?,
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        dateTimeSearched: String?,
    ) {
        // Arrange
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = ActivitySearchParamModel(
            textSearched = textSearched,
            visibilitySearched = visibilitySearched,
            availabilitySearched = availabilitySearched,
            dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(ActivityModel()))
        whenever(service.findActivitiesPage(any(), any(), any())).thenReturn(Mono.just(page))
        whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(ActivityReaderDto())),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    listOf(projectId),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("availabilitySearched", availabilitySearched),
                        Pair("dateTimeSearched", dateTimeSearched),
                    ),
                )
            )
            .header(ACCEPT_LANGUAGE, requestedLocale)
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findActivitiesPage(projectId, pageable, searchParams)
        verify(readerMapper).toDtoPage(page, locale)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findActivities throw due to wrong params`(
        activityProjectId: String?,
        pageNumber: Int?,
        pageSize: Int?,
        textSearched: String?,
        visibilitySearched: String?,
        availabilitySearched: String?,
        dateTimeSearched: String?,
        expectedMessage: String,
    ) {
        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .get()
            .uri(
                uriBuilder(
                    BASE_URL,
                    if (Objects.nonNull(activityProjectId)) listOf(activityProjectId !!) else emptyList(),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("textSearched", textSearched),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("availabilitySearched", availabilitySearched),
                        Pair("dateTimeSearched", dateTimeSearched),
                    ),
                )
            )
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedMessage)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should findActivityById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        whenever(service.findActivityById(any(), any(), anyOrNull())).thenReturn(Mono.just(ActivityModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ActivityReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .get()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ActivityReaderDto>(OK)

        verify(service).findActivityById(projectId, uuid, visibilitySearched = null)
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(movementReaderMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findActivityMovements prepare param, call service and finally cast the result`(
        pageNumber: Int?,
        pageSize: Int?,
        visibilitySearched: Boolean?,
        typeSearched: MovementTypeEnum?,
        startDateTimeSearched: String?,
        endDateTimeSearched: String?,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()
        val expectedPageNumber = pageNumber ?: 0
        val expectedPageSize = pageSize ?: 20
        val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
        val searchParams = MovementSearchParamModel(
            visibilitySearched = visibilitySearched,
            typeSearched = typeSearched,
            startDateTimeSearched = startDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
            endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
        )
        val page = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
        whenever(service.findActivityMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
        whenever(movementReaderMapper.toDtoPage(any(), any())).thenReturn(
            PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED, communicationsCount = 1))),
        )

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_HISTORY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/movements",
                    listOf(projectId, uuid),
                    listOf(
                        Pair("pageNumber", pageNumber),
                        Pair("pageSize", pageSize),
                        Pair("visibilitySearched", visibilitySearched),
                        Pair("typeSearched", typeSearched),
                        Pair("startDateTimeSearched", startDateTimeSearched),
                        Pair("endDateTimeSearched", endDateTimeSearched),
                    ),
                )
            )
            .exchange()

        // Assert
        result.body<PageModel<*>>(OK)

        verify(service).findActivityMovementsPage(projectId, uuid, pageable, searchParams)
        verify(movementReaderMapper).toDtoPage(page, locale)
        verifyNoInteractions(readerMapper)
        verifyNoInteractions(writerMapper)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findActivityMovements throw due to wrong params`(
        pageNumber: Int?,
        pageSize: Int?,
        expectedMessage: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_HISTORY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .get()
            .uri(
                uriBuilder(
                    "$BASE_URL/{id}/movements",
                    listOf(projectId, uuid),
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
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should createActivity return 200`() {
        // Arrange
        val activity = ActivityWriterDto(name = "Activity 1")

        whenever(service.createActivity(any(), any())).thenReturn(Mono.just(ActivityModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ActivityReaderDto())
        whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_C), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .post()
            .uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
            .bodyValue(activity)
            .exchange()

        // Assert
        result.body<ActivityReaderDto>(OK)

        verify(service).createActivity(any(), any())
        verify(readerMapper).toDto(any(), any())
        verify(writerMapper).toModel(activity, projectId)
        verifyNoInteractions(movementReaderMapper)
    }

    @ParameterizedTest
    @MethodSource("Wrong ActivityDto")
    fun `Should createActivity return 400`(
        activity: ActivityWriterDto,
        expectedCode: String,
    ) {
        // Act
        val result = webClient
            .authenticate()
            .post()
            .uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
            .bodyValue(activity)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }


    @Test
    fun `Should updateProjectProfile return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val activity = ActivityWriterDto(name = "Activity 1")

        whenever(service.updateActivityById(any(), any(), any(), any())).thenReturn(Mono.just(ActivityModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ActivityReaderDto())
        whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .bodyValue(activity)
            .exchange()

        // Assert
        result.body<ActivityReaderDto>(OK)

        verify(service).updateActivityById(any(), eq(projectId), eq(uuid), any())
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementReaderMapper)
        verify(writerMapper).toModel(activity, projectId)
    }

    @ParameterizedTest
    @MethodSource("Wrong ActivityDto")
    fun `Should updateActivityById return 400`(
        activity: ActivityWriterDto,
        expectedCode: String,
    ) {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = webClient
            .authenticate()
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .bodyValue(activity)
            .exchange()

        // Assert
        result.assertError(BAD_REQUEST, expectedCode)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verifyNoInteractions(service)
    }

    @Test
    fun `Should disableActivityById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.disableActivityById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ActivityModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ActivityReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ActivityReaderDto>(OK)

        verify(service).disableActivityById(any(), eq(projectId), eq(uuid))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should enableActivityById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.enableActivityById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(ActivityModel()))
        whenever(readerMapper.toDto(any(), any())).thenReturn(ActivityReaderDto())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .patch()
            .uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<ActivityReaderDto>(OK)

        verify(service).enableActivityById(any(), eq(projectId), eq(uuid))
        verify(readerMapper).toDto(any(), any())
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
    }

    @Test
    fun `Should deleteActivityById return 200`() {
        // Arrange
        val uuid = UUID.randomUUID()

        whenever(service.deleteActivityById(any(), any(), any())).thenReturn(Mono.empty())

        // Act
        val result = webClient
            .authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_D), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
            .delete()
            .uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
            .exchange()

        // Assert
        result.body<Void>(OK)

        verifyNoInteractions(readerMapper)
        verifyNoInteractions(movementReaderMapper)
        verifyNoInteractions(writerMapper)
        verify(service).deleteActivityById(any(), eq(projectId), eq(uuid))
    }
}
