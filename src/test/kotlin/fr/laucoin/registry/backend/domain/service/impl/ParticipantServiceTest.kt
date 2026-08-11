package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DISABLE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.test.ModelExt.commonGroup
import fr.laucoin.registry.backend.test.ModelExt.commonParticipant
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.ModelExt.groupId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

class ParticipantServiceTest {
	private val port: IParticipantPort = mock()
	private val projectService: IProjectService = mock()
	private val userPort: IUserPort = mock()
	private val movementPort: IMovementPort = mock()
	private val groupPort: IGroupPort = mock()
	private val service: IParticipantService = ParticipantService(
		projectService, port, userPort, movementPort, groupPort, MAX_USERS, MAX_GROUPS
	)

	private companion object {
		private const val MAX_USERS = 1
		private const val MAX_GROUPS = 2

		@JvmStatic
		fun `Should query participants and groups concurrently for a due-today panel`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(true, listOf(commonParticipant()), listOf(commonGroup())),
				Arguments.of(false, listOf(commonParticipant()), listOf(commonGroup())),
				Arguments.of(true, emptyList<ParticipantModel>(), listOf(commonGroup())),
				Arguments.of(false, listOf(commonParticipant()), emptyList<GroupModel>()),
				Arguments.of(true, emptyList<ParticipantModel>(), emptyList<GroupModel>()),
			)

		private val past = CustomDateTimeModel(LocalDate.of(2025, 9, 1))
		private val now = CustomDateTimeModel(LocalDate.of(2025, 9, 26))
		private val future = CustomDateTimeModel(LocalDate.of(2025, 10, 1))

		@JvmStatic
		fun `Should createParticipant check date, user, groups and call port create`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(commonParticipant(), 0, 0),
				Arguments.of(commonParticipant().apply { user = commonUser() }, 1, 0),
				Arguments.of(commonParticipant().apply { groups = listOf(commonGroup()) }, 0, 1),
			)
		}

		@JvmStatic
		fun `Should updateParticipantById check date, get existing participants, check user, groups and call port create`(): Stream<Arguments> {
			val groupId1 = UUID.randomUUID()
			val group1 = GroupModel().apply { id = groupId1; visible = true }
			val groupId2 = UUID.randomUUID()
			val group2 = GroupModel().apply { id = groupId2; visible = true }

			return Stream.of(
				Arguments.of(
					commonParticipant().apply { user = commonUser(); groups = listOf(group1, group2) },
					commonParticipant().apply { user = commonUser(); groups = listOf(group1, group2) },
					emptyList<GroupModel>(),
					0,
					0,
					0,
				),
				Arguments.of(
					commonParticipant().apply { groups = listOf(group1, group2) },
					commonParticipant().apply { user = commonUser(); groups = listOf(group1, group2) },
					emptyList<GroupModel>(),
					1,
					0,
					0,
				),
				Arguments.of(
					commonParticipant().apply { user = commonUser(); groups = listOf(group1, group2) },
					commonParticipant().apply { groups = listOf(group1, group2) },
					emptyList<GroupModel>(),
					0,
					0,
					0,
				),
				Arguments.of(
					commonParticipant().apply { user = commonUser(); groups = listOf(group1) },
					commonParticipant().apply { user = commonUser(); groups = listOf(group1, group2) },
					listOf(group2),
					0,
					1,
					0,
				),
				Arguments.of(
					commonParticipant().apply { user = commonUser(); groups = listOf(group1, group2) },
					commonParticipant().apply { user = commonUser(); groups = listOf(group1) },
					emptyList<GroupModel>(),
					0,
					0,
					0,
				),
				Arguments.of(
					commonParticipant().apply { startAvailability = past; endAvailability = future },
					commonParticipant().apply { startAvailability = past; endAvailability = now },
					emptyList<GroupModel>(),
					0,
					0,
					1,
				),
				Arguments.of(
					commonParticipant().apply { startAvailability = past; endAvailability = future },
					commonParticipant().apply { startAvailability = now; endAvailability = future },
					emptyList<GroupModel>(),
					0,
					0,
					1,
				),
				Arguments.of(
					commonParticipant().apply { startAvailability = past; endAvailability = future },
					commonParticipant().apply { startAvailability = now; endAvailability = now },
					emptyList<GroupModel>(),
					0,
					0,
					2,
				),
			)
		}

		@JvmStatic
		fun `Should updateParticipantById throw because of participant new date create conflict with movement`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					commonParticipant().apply { startAvailability = past; endAvailability = future },
					commonParticipant().apply { startAvailability = now; endAvailability = future },
				),
				Arguments.of(
					commonParticipant().apply { startAvailability = past; endAvailability = future },
					commonParticipant().apply { startAvailability = past; endAvailability = now },
				),
			)
		}

		@JvmStatic
		fun `Should disableParticipantById call existing participant, check if the last member of one of his groups and call port update`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(emptyList<GroupModel>(), 0),
				Arguments.of(listOf(commonGroup()), 1),
			)
		}
	}

	@Test
	fun `Should findParticipantsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ParticipantSearchParamModel()

		whenever(port.findPage(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findParticipantsPage(projectId, pageable, params).block()

		// Assert
		verify(port).findPage(projectId, pageable, params, emptyList())
	}

	@Test
	fun `Should findParticipantsByIds call port findAllByIds`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.just(commonParticipant()))

		// Act
		service.findParticipantsByIds(projectId, listOf(participantId), onlyVisible).blockFirst()

		// Assert
		verify(port).findAllByIds(projectId, listOf(participantId), onlyVisible)
	}

	@Test
	fun `Should findBirthdays call port findAllByIds`() {
		// Arrange
		val onlyVisible = true
		val limit = 5

		whenever(port.findBirthdays(any(), any(), any())).thenReturn(Flux.just(commonParticipant()))

		// Act
		service.findBirthdays(projectId, limit).blockFirst()

		// Assert
		verify(port).findBirthdays(projectId, onlyVisible, limit)
	}

	/**
	 * The two "due today" reads are issued CONCURRENTLY: nothing in either side
	 * feeds the other, so chaining them would make the panel wait for their sum.
	 * Both are verified to have run, and the two sides come back separated —
	 * the panel lists groups and participants differently.
	 */
	@ParameterizedTest
	@MethodSource
	fun `Should query participants and groups concurrently for a due-today panel`(
		arriving: Boolean,
		participants: List<ParticipantModel>,
		groups: List<GroupModel>,
	) {
		// Arrange
		val limit = 5
		whenever(port.findArrivingToday(any(), anyOrNull(), any())).thenReturn(Flux.fromIterable(participants))
		whenever(port.findDepartingToday(any(), anyOrNull(), any())).thenReturn(Flux.fromIterable(participants))
		whenever(groupPort.findArrivingToday(any(), anyOrNull(), any())).thenReturn(Flux.fromIterable(groups))
		whenever(groupPort.findDepartingToday(any(), anyOrNull(), any())).thenReturn(Flux.fromIterable(groups))

		// Act
		val result = (if (arriving) service.findArrivalsToday(projectId, limit)
		else service.findDeparturesToday(projectId, limit)).block()

		// Assert
		assertEquals(participants, result?.first)
		assertEquals(groups, result?.second)
		if (arriving) {
			verify(port).findArrivingToday(projectId, true, limit)
			verify(groupPort).findArrivingToday(projectId, true, limit)
			verify(port, never()).findDepartingToday(any(), anyOrNull(), any())
			verify(groupPort, never()).findDepartingToday(any(), anyOrNull(), any())
		} else {
			verify(port).findDepartingToday(projectId, true, limit)
			verify(groupPort).findDepartingToday(projectId, true, limit)
			verify(port, never()).findArrivingToday(any(), anyOrNull(), any())
			verify(groupPort, never()).findArrivingToday(any(), anyOrNull(), any())
		}
	}

	@Test
	fun `Should findParticipantById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonParticipant()))

		// Act
		service.findParticipantById(projectId, participantId, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, participantId, onlyVisible)
	}

	@Test
	fun `Should findParticipantById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findParticipantById(projectId, participantId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(participantId.toString(), result.args?.first())

		verify(port).findById(projectId, participantId, onlyVisible)
	}

	@Test
	fun `Should searchUsers call userPort findWithLimit`() {
		// Arrange
		val textSearched = "text"
		val expectedSearch = UserSearchParamModel(textSearched, visibilitySearched = true)

		whenever(userPort.findWithLimit(any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchUsersByText(projectId, textSearched).blockFirst()

		// Assert
		verify(userPort).findWithLimit(MAX_USERS, expectedSearch)
	}

	@Test
	fun `Should searchGroups call groupPort findWithLimit`() {
		// Arrange
		val textSearched = "text"
		val expectedSearch = GroupSearchParamModel(textSearched, visibilitySearched = true)

		whenever(groupPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchGroupsByText(projectId, textSearched).blockFirst()

		// Assert
		verify(groupPort).findWithLimit(MAX_GROUPS, projectId, expectedSearch)
	}

	@Test
	fun `Should findParticipantMovementsPage call movementPort findPageByParticipantId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = IN)

		whenever(movementPort.findPageByParticipantId(any(), any(), any(), any())).thenReturn(Mono.empty())

		// Act
		service.findParticipantMovementsPage(projectId, participantId, pageable, params).block()

		// Assert
		verify(movementPort).findPageByParticipantId(projectId, participantId, pageable, params)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should createParticipant check date, user, groups and call port create`(
		participant: ParticipantModel,
		expectedUserVerification: Int,
		expectedGroupVerification: Int,
	) {
		// Arrange
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.create(any())).thenReturn(Mono.just(participant))
		whenever(port.findByUserId(any(), any())).thenReturn(Flux.empty())
		whenever(groupPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(*participant.groups.toTypedArray()))

		// Act
		service.createParticipant(currentUser(), participant).block()

		// Assert
		verify(projectService).validateDateTimes(
			projectId, null, null, PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
		verify(port, times(expectedUserVerification))
			.findByUserId(projectId, participant.user?.id ?: UUID.randomUUID())
		verify(groupPort, times(expectedGroupVerification))
			.findAllByIds(projectId, participant.groups.mapNotNull { it.id }, null)
		verify(port).create(participant)
	}

	@Test
	fun `Should createParticipant check date, user and throw because participant is already linked`() {
		// Arrange
		val participant = commonParticipant().apply { user = commonUser() }
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.findByUserId(any(), any())).thenReturn(Flux.just(participant))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createParticipant(currentUser(), participant).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER, result.message)

		verify(projectService).validateDateTimes(
			projectId, null, null, PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(port).findByUserId(projectId, participant.user?.id ?: UUID.randomUUID())
		verify(groupPort, never()).findAllByIds(any(), any(), anyOrNull())
		verify(port, never()).create(any())
	}

	@Test
	fun `Should createParticipant check date, user, groups and throw because group is not found`() {
		// Arrange
		val participant = commonParticipant().apply { groups = listOf(commonGroup()) }

		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.create(any())).thenReturn(Mono.just(participant))
		whenever(port.findByUserId(any(), any())).thenReturn(Flux.empty())
		whenever(groupPort.findAllByIds(any(), any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createParticipant(currentUser(), participant).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_PROJECT, result.message)

		verify(projectService).validateDateTimes(
			projectId, null, null, PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(port, never()).findByUserId(any(), any())
		verify(groupPort).findAllByIds(projectId, participant.groups.mapNotNull { it.id }, null)
		verify(port, never()).create(any())
	}

	@Test
	fun `Should createParticipant check date, user, groups and throw because group is not visible`() {
		// Arrange
		val participant = commonParticipant().apply { groups = listOf(commonGroup()) }

		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.create(any())).thenReturn(Mono.just(participant))
		whenever(port.findByUserId(any(), any())).thenReturn(Flux.empty())
		whenever(groupPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(commonGroup().apply { visible = false }))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.createParticipant(currentUser(), participant).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(PARTICIPANT_GROUPS_NOT_VISIBLE, result.message)
		verify(projectService).validateDateTimes(
			projectId, null, null, PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
		)
		verify(port, never()).findByUserId(any(), any())
		verify(groupPort).findAllByIds(projectId, participant.groups.mapNotNull { it.id }, null)
		verify(port, never()).create(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateParticipantById check date, get existing participants, check user, groups and call port create`(
		oldParticipant: ParticipantModel,
		newParticipant: ParticipantModel,
		newGroups: List<GroupModel>,
		expectedUserVerification: Int,
		expectedGroupVerification: Int,
		expectedMovementVerification: Int,
	) {
		// Arrange
		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldParticipant))
		whenever(port.update(any())).thenReturn(Mono.just(newParticipant))
		whenever(port.findByUserId(any(), any())).thenReturn(Flux.empty())
		whenever(movementPort.countAllByParticipantId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(groupPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(*newGroups.toTypedArray()))

		// Act
		service.updateParticipantById(currentUser(), projectId, participantId, newParticipant).block()

		// Assert
		verify(projectService).validateDateTimes(
			eq(projectId), anyOrNull(), anyOrNull(), eq(PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE)
		)
		verify(port).findById(projectId, participantId, visibilitySearched = null)
		verify(port, times(expectedUserVerification))
			.findByUserId(eq(projectId), any())
		verify(groupPort, times(expectedGroupVerification))
			.findAllByIds(projectId, newGroups.mapNotNull { it.id }, visibilitySearched = null)
		verify(movementPort, times(expectedMovementVerification))
			.countAllByParticipantId(eq(projectId), eq(participantId), any())
		verify(port).update(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateParticipantById throw because of participant new date create conflict with movement`(
		oldParticipant: ParticipantModel,
		newParticipant: ParticipantModel,
	) {
		// Arrange
		val movementCountOutOfRange = 1L

		whenever(projectService.validateDateTimes(any(), anyOrNull(), anyOrNull(), any()))
			.thenReturn(Mono.just(projectId))
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(oldParticipant))
		whenever(movementPort.countAllByParticipantId(any(), any(), any()))
			.thenReturn(Mono.just(movementCountOutOfRange))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateParticipantById(currentUser(), projectId, participantId, newParticipant).block()
		}) as RegistryException

		// Assert
		assertEquals(result.status, UNPROCESSABLE_CONTENT)
		assertEquals(result.code, PARTICIPANT_OUT_OF_MOVEMENT_DATETIME)
		assertEquals(result.args, arrayListOf(movementCountOutOfRange))

		verify(projectService).validateDateTimes(
			eq(projectId), anyOrNull(), anyOrNull(), eq(PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE),
		)
		verify(port).findById(projectId, participantId, visibilitySearched = null)
		verify(movementPort).countAllByParticipantId(eq(projectId), eq(participantId), any())
		verify(port, never()).update(any())
	}

	/**
	 * The last-member guard reads `membersCount`, so that is what these stubs carry:
	 * `findAllByIds` selects the member COUNTS and leaves `members` at its empty
	 * default, and a stub populating `members` instead describes a row the
	 * repository never returns — which is how the guard once passed for the wrong
	 * reason.
	 */
	@ParameterizedTest
	@MethodSource
	fun `Should disableParticipantById call existing participant, check if the last member of one of his groups and call port update`(
		participantGroups: List<GroupModel>,
		expectedCallGroupVerification: Int,
	) {
		// Arrange
		val participant = commonParticipant().apply { groups = participantGroups }
		val groups = participantGroups
			.map { GroupModel().apply { membersCount = 2 } }
			.toTypedArray()

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
		whenever(groupPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(*groups))
		whenever(port.update(any())).thenReturn(Mono.just(participant))

		// Act
		service.disableParticipantById(currentUser(), projectId, participantId).block()

		// Assert
		verify(port).findById(projectId, participantId, visibilitySearched = true)
		verify(groupPort, times(expectedCallGroupVerification)).findAllByIds(
			projectId, participantGroups.mapNotNull { it.id }, visibilitySearched = null,
		)
		verify(port).update(participant.apply { visible = false })
	}

	@Test
	fun `Should disableParticipantById call existing participant, check if the last member of one of his groups and throw`() {
		// Arrange
		val participant = commonParticipant().apply { groups = listOf(commonGroup()) }

		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
		whenever(groupPort.findAllByIds(any(), any(), anyOrNull()))
			.thenReturn(Flux.just(commonGroup().apply { membersCount = 1 }))
		whenever(port.update(any())).thenReturn(Mono.just(participant))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.disableParticipantById(currentUser(), projectId, participantId).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PARTICIPANT_DISABLE_LAST_GROUP_MEMBER, result.message)

		verify(port).findById(projectId, participantId, visibilitySearched = true)
		verify(groupPort).findAllByIds(projectId, listOf(groupId), visibilitySearched = null)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should enableParticipantById call existing participant and call port update`() {
		// Arrange
		val participant = commonParticipant().apply { visible = false }
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(participant))
		whenever(port.update(any())).thenReturn(Mono.just(participant))

		// Act
		service.enableParticipantById(currentUser(), projectId, participantId).block()

		// Assert
		verify(port).findById(projectId, participantId, visibilitySearched = false)
		verify(port).update(commonParticipant())
	}

	@Test
	fun `Should deleteParticipantById call existing participant, check no movement, and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonParticipant()))
		whenever(movementPort.countAllByParticipantId(any(), any(), any())).thenReturn(Mono.just(0))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteParticipantById(currentUser(), projectId, participantId).block()

		// Assert
		verify(port).findById(projectId, participantId, visibilitySearched = null)
		verify(movementPort).countAllByParticipantId(projectId, participantId, MovementSearchParamModel())
		verify(port).deleteById(participantId)
	}

	@Test
	fun `Should deleteParticipantById call existing participant, throw if movements are linked`() {
		// Arrange
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonParticipant()))
		whenever(movementPort.countAllByParticipantId(any(), any(), any())).thenReturn(Mono.just(1))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.deleteParticipantById(currentUser(), projectId, participantId).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PARTICIPANT_DELETE_HAS_MOVEMENT, result.message)

		verify(port).findById(projectId, participantId, visibilitySearched = null)
		verify(movementPort).countAllByParticipantId(projectId, participantId, MovementSearchParamModel())
		verify(port, never()).deleteById(any())
	}

	@Test
	fun `Should purgeParticipantsIfNecessary call unused participant since a date, and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()

		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(uuid1, uuid2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeParticipantsIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findUnusedSince(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeParticipantsIfNecessary call unused participant since a date, and not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH
		whenever(port.findUnusedSince(any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeParticipantsIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findUnusedSince(date)
		verify(port, never()).deleteById(any())
	}
}
