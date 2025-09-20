package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ParticipantWriterDtoMapper
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ParticipantControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IParticipantService

	@MockitoBean
	private lateinit var readerMapper: ParticipantReaderDtoMapper

	@MockitoBean
	private lateinit var groupReaderMapper: GroupWithoutMemberReaderDtoMapper

	@MockitoBean
	private lateinit var movementReaderMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var partialUserReaderMapper: PartialUserReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ParticipantWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/projects/{projectId}/participants"

		@JvmStatic
		fun `Should findParticipants return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null, null, null),
			Arguments.of(null, 0, null, null, null, null, null),
			Arguments.of(null, null, 200, null, null, null, null),
			Arguments.of(null, null, null, null, null, null, null),
			Arguments.of(null, null, null, "text", null, null, null),
			Arguments.of(null, null, null, null, true, null, null),
			Arguments.of(null, null, null, null, null, PresenceStatusEnum.IN, null),
			Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should findParticipants throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Should findParticipantMovements return 200`(): Stream<Arguments> {
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
		fun `Should findParticipantMovements throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Wrong ParticipantDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				ParticipantWriterDto(lastName = "DOE", birthday = LocalDate.EPOCH),
				PARTICIPANT_FIRST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "", lastName = "DOE", birthday = LocalDate.EPOCH),
				PARTICIPANT_FIRST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(
					firstName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
					lastName = "DOE",
					birthday = LocalDate.EPOCH
				),
				PARTICIPANT_FIRST_NAME_TOO_LONG,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "John", birthday = LocalDate.EPOCH),
				PARTICIPANT_LAST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "John", lastName = "", birthday = LocalDate.EPOCH),
				PARTICIPANT_LAST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(
					firstName = "John",
					lastName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
					birthday = LocalDate.EPOCH
				),
				PARTICIPANT_LAST_NAME_TOO_LONG,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.MAX),
				PARTICIPANT_BIRTHDAY_FUTURE,
			),
			Arguments.of(
				ParticipantWriterDto(
					firstName = "John",
					lastName = "DOE",
					birthday = LocalDate.EPOCH,
					startAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
				),
				PARTICIPANT_START_LATER_THAN_END,
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipants return 200`(
		requestedLocale: String?,
		pageNumber: Int?,
		pageSize: Int?,
		textSearched: String?,
		visibilitySearched: Boolean?,
		statusSearched: PresenceStatusEnum?,
		dateTimeSearched: String?,
	) {
		// Arrange
		val expectedPageNumber = pageNumber ?: 0
		val expectedPageSize = pageSize ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = ParticipantSearchParamModel(
			textSearched = textSearched,
			visibilitySearched = visibilitySearched,
			statusSearched = statusSearched,
			dateTimeSearched = dateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findParticipantsPage(any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
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
						Pair("statusSearched", statusSearched),
						Pair("dateTimeSearched", dateTimeSearched),
					),
				)
			)
			.header(ACCEPT_LANGUAGE, requestedLocale)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findParticipantsPage(projectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(page, Locale.ENGLISH)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipants throw due to wrong params`(
		pageNumber: Int?,
		pageSize: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("pageNumber", pageNumber),
						Pair("pageSize", pageSize),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(service)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findParticipantById(any(), any(), anyOrNull())).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).findParticipantById(projectId, uuid, visibilitySearched = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipantMovements return 200`(
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
			startDateTimeSearched = startDateTimeSearched?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findParticipantMovementsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(movementReaderMapper.toDtoPage(any(), any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R))
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

		verify(service).findParticipantMovementsPage(projectId, uuid, pageable, searchParams)
		verify(movementReaderMapper).toDtoPage(page, Locale.ENGLISH)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(groupReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipantMovements throw due to wrong params`(
		pageNumber: Int?,
		pageSize: Int?,
		expectedMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R))
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
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(groupReaderMapper)
	}

	@Test
	fun `Should searchUsers return 200`() {
		// Arrange
		val searched = "John"
		val user = UserModel()
		whenever(service.searchUsersByText(any(), anyOrNull())).thenReturn(Flux.just(user))
		whenever(partialUserReaderMapper.toDto(any(), any())).thenReturn(PartialUserReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(
				uriBuilder("$BASE_URL/search/users", listOf(projectId), listOf(Pair("textSearched", searched)))
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchUsersByText(projectId, searched)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verify(partialUserReaderMapper).toDto(any(), any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchGroups return 200`() {
		// Arrange
		val searched = "Group"
		val group = GroupModel()
		whenever(service.searchGroupsByText(any(), anyOrNull())).thenReturn(Flux.just(group))
		whenever(groupReaderMapper.toDto(any(), any())).thenReturn(GroupReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(
				uriBuilder("$BASE_URL/search/groups", listOf(projectId), listOf(Pair("textSearched", searched)))
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchGroupsByText(projectId, searched)
		verifyNoInteractions(readerMapper)
		verify(groupReaderMapper).toDto(any(), any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createParticipant return 200`() {
		// Arrange
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)
		whenever(service.createParticipant(any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(partialUserReaderMapper)
		verify(writerMapper).toModel(participant, projectId)
		verify(service).createParticipant(any(), any())
	}

	@Test
	fun `Should createParticipant return 200 with User and Groups`() {
		// Arrange
		val participant = ParticipantWriterDto(
			firstName = "John",
			lastName = "DOE",
			birthday = LocalDate.EPOCH,
			userId = UUID.randomUUID(),
			groupIds = listOf(UUID.randomUUID(), UUID.randomUUID())
		)
		whenever(service.createParticipant(any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(partialUserReaderMapper)
		verify(writerMapper).toModel(participant, projectId)
		verify(service).createParticipant(any(), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong ParticipantDto")
	fun `Should createParticipant return 400`(
		participant: ParticipantWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}


	@Test
	fun `Should updateParticipant return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)

		whenever(service.updateParticipantById(any(), any(), any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(partialUserReaderMapper)
		verify(writerMapper).toModel(participant, projectId)
		verify(service).updateParticipantById(any(), eq(projectId), eq(uuid), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong ParticipantDto")
	fun `Should updateParticipantById return 400`(
		participant: ParticipantWriterDto,
		expectedCode: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should disableParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(
			service.disableParticipantById(
				any(),
				eq(projectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).disableParticipantById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should enableParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(
			service.enableParticipantById(
				any(),
				eq(projectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).enableParticipantById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should deleteParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteParticipantById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteParticipantById(any(), eq(projectId), eq(uuid))
	}
}
