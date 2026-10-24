package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_MAX_IS_HIGHER_THAN_MIN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DESCRIPTION_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_MIN_HIGHER_THAN_MAX
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_NOT_SUPPORTED
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
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.NumericRangeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ActivityReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ActivityWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.hiddenElementsAuthorities
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ActivityControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IActivityService

	@MockitoBean
	private lateinit var readerMapper: ActivityReaderDtoMapper

	@MockitoBean
	private lateinit var movementReaderMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ActivityWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/activities"

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
		fun `Should findActivities throw due to wrong duration or participants`(): Stream<Arguments> = Stream.of(
			Arguments.of("not a duration", null, null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of(null, "1 hour", null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of("PT99999999H", null, null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of("-PT1H", null, null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of(null, "Infinity", null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of("PT8808H", null, null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of("P367D", null, null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of("P".padEnd(33, '1'), null, null, PARAMETER_TOO_LONG),
			Arguments.of(null, "P".padEnd(33, '1'), null, PARAMETER_TOO_LONG),
			Arguments.of("PT1H;DROP", null, null, PARAMETER_FORMAT_INVALID),
			Arguments.of(null, "PT1H<x>", null, PARAMETER_FORMAT_INVALID),
			Arguments.of(null, "P367D", null, ACTIVITY_DURATION_FORMAT_FAILED),
			Arguments.of("PT2H", "PT1H", null, ACTIVITY_DURATION_MIN_HIGHER_THAN_MAX),
			Arguments.of(null, null, 0, ACTIVITY_ALLOWED_PARTICIPANTS_TOO_LOW),
		)

		@JvmStatic
		fun `Should findActivities forward the sort to the service`(): Stream<Arguments> = Stream.of(
			Arguments.of("name", SortModel("name", false)),
			Arguments.of("-name", SortModel("name", true)),
			Arguments.of("-startAvailability", SortModel("startAvailability", true)),
		)

		@JvmStatic
		fun `Should findActivities throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of("not uuid", null, null, null, null, null, null, PARAMETER_TYPE_MISMATCH),
				Arguments.of(
					defaultProjectId.toString(),
					-1,
					null,
					null,
					null,
					null,
					null,
					PAGE_NUMBER_IS_LOWER_THAN_ZERO
				),
				Arguments.of(defaultProjectId.toString(), null, 0, null, null, null, null, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					201,
					null,
					null,
					null,
					null,
					PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					"not boolean",
					null,
					null,
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					null,
					"not boolean",
					null,
					PARAMETER_TYPE_MISMATCH
				),
				Arguments.of(
					defaultProjectId.toString(),
					null,
					null,
					null,
					null,
					null,
					"2024/11/14 18:34:33",
					PARAMETER_TYPE_MISMATCH
				),
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
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
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
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		isAvailable: Boolean?,
		at: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = ActivitySearchParamModel(
			query = query,
			isVisible = isVisible,
			isAvailable = isAvailable,
			at = at?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ActivityModel()))
		whenever(service.findActivitiesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ActivityReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("isAvailable", isAvailable),
						Pair("at", at),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findActivitiesPage(defaultProjectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(expectedPage)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findActivities throw due to wrong params`(
		activityProjectId: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: String?,
		isAvailable: String?,
		at: String?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					if (Objects.nonNull(activityProjectId)) listOf(activityProjectId!!) else emptyList(),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("isAvailable", isAvailable),
						Pair("at", at),
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
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).findActivityById(defaultProjectId, uuid, isVisible = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(movementReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findActivityMovements prepare param, call service and finally cast the result`(
		page: Int?,
		size: Int?,
		isVisible: Boolean?,
		type: MovementTypeEnum?,
		from: String?,
		to: String?,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = MovementSearchParamModel(
			isVisible = isVisible,
			type = type,
			from = from?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			to = to?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findActivityMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(movementReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), 
				buildAuthority(REGISTRY_PROJECT_ACTIVITY_HISTORY_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("isVisible", isVisible),
						Pair("type", type),
						Pair("from", from),
						Pair("to", to),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findActivityMovementsPage(defaultProjectId, uuid, pageable, searchParams)
		verify(movementReaderMapper).toDtoPage(expectedPage)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findActivityMovements throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), 
				buildAuthority(REGISTRY_PROJECT_ACTIVITY_HISTORY_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY)
			)
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
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
	fun `Should createActivity return 201`() {
		// Arrange
		val activity = ActivityWriterDto(name = "Activity 1")

		whenever(service.createActivity(any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_C), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(CREATED)

		verify(service).createActivity(any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(activity, defaultProjectId)
		verifyNoInteractions(movementReaderMapper)
	}

	@Test
	fun `Should createActivity expose the location of the new activity`() {
		// Arrange
		val activity = ActivityWriterDto(name = "Activity 1")
		val createdId = UUID.randomUUID()
		whenever(service.createActivity(any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto().apply { this.id = createdId })
		whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_C), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.expectStatus().isCreated
			.expectHeader().valueEquals("Location", "/api/v2/projects/$defaultProjectId/activities/$createdId")
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
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
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
	fun `Should updateActivity return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val activity = ActivityWriterDto(name = "Activity 1")

		whenever(service.updateActivityById(any(), any(), any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).updateActivityById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verify(writerMapper).toModel(activity, defaultProjectId)
	}

	@Test
	fun `Should updateActivity accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val activity = ActivityWriterDto(name = "Activity 1")

		whenever(service.updateActivityById(any(), any(), any(), any())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())
		whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())
		whenever(service.findActivityById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(ActivityModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(activity)
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).updateActivityById(any(), eq(defaultProjectId), eq(uuid), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verify(writerMapper).toModel(activity, defaultProjectId)
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
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

		whenever(
			service.disableActivityById(
				any(),
				eq(defaultProjectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).disableActivityById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableActivityById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(
			service.enableActivityById(
				any(),
				eq(defaultProjectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ActivityReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_D), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ActivityReaderDto>(OK)

		verify(service).enableActivityById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteActivityById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteActivityById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_D), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteActivityById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should findActivities forward duration and participants filters to the service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = ActivitySearchParamModel(
			query = null,
			minDuration = 30.minutes,
			maxDuration = 2.hours,
			participants = 8,
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ActivityModel()))
		whenever(service.findActivitiesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ActivityReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(Pair("minDuration", "PT30M"), Pair("maxDuration", "PT2H"), Pair("participants", 8)),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findActivitiesPage(defaultProjectId, pageable, searchParams)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findActivities throw due to wrong duration or participants`(
		minDuration: String?,
		maxDuration: String?,
		participants: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("minDuration", minDuration),
						Pair("maxDuration", maxDuration),
						Pair("participants", participants),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(service)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findActivities forward the sort to the service`(sort: String, expected: SortModel) {
		// Arrange
		val pageable = PageableModel(0, 20)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ActivityModel()))
		whenever(service.findActivitiesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ActivityReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("sort", sort))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findActivitiesPage(
			defaultProjectId,
			pageable,
			ActivitySearchParamModel(query = null, sort = expected)
		)
	}

	@Test
	fun `Should findActivities throw due to unsupported sort field`() {
		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("sort", "-password"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_NOT_SUPPORTED)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should findActivityById expose the ETag of the last edition`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val lastEdition = ZonedDateTime.parse("2026-10-13T10:02:11.123456Z")
		whenever(service.findActivityById(any(), any(), anyOrNull())).thenReturn(Mono.just(ActivityModel()))
		whenever(readerMapper.toDto(any())).thenReturn(
			ActivityReaderDto().apply { this.lastEdition = HistoryModel(dateTime = lastEdition) },
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isOk.expectHeader().valueEquals("ETag", "\"1791885731123\"")
	}

	@Test
	fun `Should updateActivityById throw when the activity changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(writerMapper.toModel(any(), any())).thenReturn(ActivityModel())
		whenever(service.findActivityById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(ActivityModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_U), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(ActivityWriterDto(name = "Activity 1"))
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateActivityById(any(), any(), any(), any())
	}

	@ParameterizedTest
	@CsvSource("0,true", "1,true", "2,false")
	fun `Should findActivities expose whether another page follows`(page: Int, expectedHasNext: Boolean) {
		// Arrange
		val pageable = PageableModel(page * 20, 20)
		val expectedPage = PageModel(pageable, totalElements = 45, listOf(ActivityModel()))
		whenever(service.findActivitiesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(
				pageable,
				totalElements = 45,
				listOf(ActivityReaderDto())
			)
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("page", page))))
			.exchange()

		// Assert
		result.expectBody().jsonPath("$.hasNext").isEqualTo(expectedHasNext)
	}

	@ParameterizedTest
	@ValueSource(strings = ["PT0S", "P366D", "PT8784H"])
	fun `Should findActivities accept the duration bounds`(duration: String) {
		// Arrange
		val pageable = PageableModel(0, 20)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ActivityModel()))
		whenever(service.findActivitiesPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(
				pageable,
				totalElements = 1,
				listOf(ActivityReaderDto())
			)
		)

		// Act
		val result = webClient
			.authenticate(*hiddenElementsAuthorities(), buildAuthority(REGISTRY_PROJECT_ACTIVITY_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(Pair("minDuration", duration), Pair("maxDuration", "P366D"))
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)
	}

	@Test
	fun `Should createActivity refuse a duration over 32 characters`() {
		// Arrange
		val activity = ActivityWriterDto(name = "Activity 1", duration = "P".padEnd(33, '1'))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_C), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createActivity refuse a duration with forbidden characters`() {
		// Arrange
		val activity = ActivityWriterDto(name = "Activity 1", duration = "PT1H;DROP")

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_ACTIVITY_C), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(activity)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_FORMAT_INVALID)
		verifyNoInteractions(service)
	}
}
