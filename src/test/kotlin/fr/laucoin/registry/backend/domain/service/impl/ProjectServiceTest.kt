package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.test.ModelExt.commonProject
import fr.laucoin.registry.backend.test.ModelExt.commonProjectProfile
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ProjectServiceTest {
	private val port: IProjectPort = mock()
	private val projectProfileService: IUserProjectProfileService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val roleService: IRoleService = mock()
	private val service: IProjectService = ProjectService(
		port,
		projectProfileService,
		transactionalOperator,
		roleService
	)

	private companion object {
		private val MIN_DATE = LocalDate.MIN
		private val MAX_DATE = LocalDate.MAX

		private val MIDNIGHT_PLUS_ONE_NANOSECOND = OffsetTime.of(0, 0, 0, 1, ZoneOffset.of("Z"))
		private val MIDNIGHT_LESS_ONE_NANOSECOND = OffsetTime.of(23, 59, 59, 0, ZoneOffset.of("Z"))

		@JvmStatic
		fun `Should validateDateTime call port findById and validate the request date is in project range`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null),
				Arguments.of(null, null, CustomDateTimeModel(MIN_DATE)),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MIN_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel.MIN),
				Arguments.of(CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel.MAX),
				Arguments.of(CustomDateTimeModel.MIN, CustomDateTimeModel.MAX, CustomDateTimeModel.MIN),
				Arguments.of(CustomDateTimeModel.MIN, CustomDateTimeModel.MAX, CustomDateTimeModel.MAX),
			)
		}

		@JvmStatic
		fun `Should validateDateTime call port findById and throw on invalid date`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MAX_DATE), CustomDateTimeModel(MIN_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE, MIDNIGHT_PLUS_ONE_NANOSECOND),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel.MIN,
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE, MIDNIGHT_LESS_ONE_NANOSECOND),
					CustomDateTimeModel.MAX,
				),
			)
		}

		@JvmStatic
		fun `Should validateDateTimes call port findById and validate the request dates are in project range`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null, null),
				Arguments.of(null, null, CustomDateTimeModel(MIN_DATE), CustomDateTimeModel(MAX_DATE)),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel.MIN,
					CustomDateTimeModel(MIN_DATE, OffsetTime.MAX),
				),
				Arguments.of(
					CustomDateTimeModel.MIN, CustomDateTimeModel.MAX, CustomDateTimeModel.MIN, CustomDateTimeModel.MAX,
				),
			)
		}

		@JvmStatic
		fun `Should validateDateTimes call port findById and throw on invalid dates`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MIN_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE, MIDNIGHT_PLUS_ONE_NANOSECOND),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel.MIN,
					CustomDateTimeModel(MAX_DATE),
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE, MIDNIGHT_LESS_ONE_NANOSECOND),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel.MAX,
				),
			)
		}

		@JvmStatic
		fun `Should updateProjectById call port findById, validDateTime and update`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null, null, 0),
				Arguments.of(
					null, null,
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					1,
				),
				Arguments.of(
					null,
					CustomDateTimeModel.EPOCH,
					CustomDateTimeModel.EPOCH,
					null,
					1,
				),
				Arguments.of(
					CustomDateTimeModel.EPOCH,
					null,
					null,
					CustomDateTimeModel.EPOCH,
					1,
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					0,
				),
				Arguments.of(
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					CustomDateTimeModel.MIN,
					CustomDateTimeModel(MIN_DATE, OffsetTime.MAX),
					1,
				),
				Arguments.of(
					CustomDateTimeModel.MIN,
					CustomDateTimeModel.MAX,
					CustomDateTimeModel.MIN,
					CustomDateTimeModel.MAX,
					0,
				),
				Arguments.of(
					CustomDateTimeModel.EPOCH,
					CustomDateTimeModel.EPOCH,
					CustomDateTimeModel(MIN_DATE),
					CustomDateTimeModel(MAX_DATE),
					0,
				),
			)
		}
	}

	@Test
	fun `Should findProjectsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectSearchParamModel()
		val withProfile = false

		whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(listOf(REGISTRY_PROJECT_R))
		whenever(port.findPage(any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findProjectsPage(currentUser(REGISTRY_PROJECT_R), pageable, withProfile, params).block()

		// Assert
		verify(port).findPage(pageable, params)
		verify(port, never()).findPage(any(), any(), any())
	}

	@Test
	fun `Should findProjectsPage call port findPage for currentUserProject`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val withProfile = true
		val params = ProjectSearchParamModel()

		whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(emptyList())
		whenever(roleService.getProjectIdsFromCurrentUserProfiles(any())).thenReturn(emptyList())
		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findProjectsPage(currentUser(), pageable, withProfile, params).block()

		// Assert
		verify(port).findPage(emptyList(), pageable, params)
		verify(port, never()).findPage(any(), any())
	}

	@Test
	fun `Should findProjectById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(commonProject()))

		// Act
		service.findProjectById(projectId, onlyVisible).block()

		// Assert
		verify(port).findById(projectId, onlyVisible)
	}

	@Test
	fun `Should findProjectById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findProjectById(projectId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(projectId.toString(), result.args?.first())

		assertEquals(commonProject().id.toString(), result.args?.first())

		verify(port).findById(projectId, onlyVisible)
	}

	@Test
	fun `Should availableProjectOptions not throw`() {
		// Act
		assertDoesNotThrow { service.availableProjectOptions().blockFirst() }

		// Assert
		verifyNoInteractions(port)
		verifyNoInteractions(projectProfileService)
		verifyNoInteractions(transactionalOperator)
		verifyNoInteractions(roleService)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTime call port findById and validate the request date is in project range`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		dateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = commonProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = service.validateDateTime(projectId, dateTime?.copy(), errorMessage).block()

		// Assert
		assertEquals(projectId, result)

		verify(port).findById(projectId, visibilitySearched = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTime call port findById and throw on invalid date`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		dateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = commonProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.validateDateTime(projectId, dateTime?.copy(), errorMessage).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_ENTITY, result.status)
		assertEquals(errorMessage, result.message)
		assertEquals(3, result.args?.size)

		verify(port).findById(projectId, visibilitySearched = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTimes call port findById and validate the request dates are in project range`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		startDateTime: CustomDateTimeModel?,
		endDateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = commonProject().apply { begin = projectBeginDateTime; end = projectEndDateTime }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = service.validateDateTimes(projectId, startDateTime, endDateTime, errorMessage).block()

		// Assert
		assertEquals(projectId, result)

		verify(port).findById(projectId, visibilitySearched = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTimes call port findById and throw on invalid dates`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		startDateTime: CustomDateTimeModel?,
		endDateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = commonProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.validateDateTimes(projectId, startDateTime?.copy(), endDateTime?.copy(), errorMessage).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_ENTITY, result.status)
		assertEquals(errorMessage, result.message)
		assertEquals(4, result.args?.size)

		verify(port).findById(projectId, visibilitySearched = null)
	}

	@Test
	fun `Should createProject call profile service createUserProjectProfileFromProject and port create`() {
		// Arrange
		whenever(projectProfileService.createUserProjectProfileFromProject(any(), any()))
			.thenReturn(Mono.just(commonProjectProfile()))
		whenever(port.create(any())).thenReturn(Mono.just(commonProject()))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		service.createProject(currentUser(), commonProject()).block()

		// Assert
		verify(projectProfileService).createUserProjectProfileFromProject(currentUser(), commonProject())
		verify(port).create(commonProject())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should updateProjectById call port findById, validDateTime and update`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		newProjectBeginDateTime: CustomDateTimeModel?,
		newProjectEndDateTime: CustomDateTimeModel?,
		expectedVerificationCall: Int,
	) {
		// Arrange
		val projectToUpdate = commonProject()
			.apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val projectUpdated = commonProject()
			.apply { begin = newProjectBeginDateTime?.copy(); end = newProjectEndDateTime?.copy() }

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(projectToUpdate))
		whenever(port.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(true))
		whenever(port.update(any())).thenReturn(Mono.just(projectUpdated))

		// Act
		service.updateProjectById(currentUser(), projectId, projectUpdated).block()

		// Assert
		verify(port).findById(projectId, visibilitySearched = null)
		verify(port, times(expectedVerificationCall))
			.validDateTime(eq(projectId), anyOrNull(), anyOrNull())
		verify(port).update(any())
	}

	@Test
	fun `Should updateProjectById call port findById, validDateTime and throw due to date conflict`() {
		// Arrange
		val dateTime = CustomDateTimeModel(LocalDate.EPOCH)
		val projectUpdated = commonProject().apply { begin = dateTime; end = dateTime }

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(commonProject()))
		whenever(port.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(false))
		whenever(port.update(any())).thenReturn(Mono.just(projectUpdated))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateProjectById(currentUser(), projectId, projectUpdated).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PROJECT_DATE_CONFLICT_WITH_ELEMENTS, result.message)
		assertNull(result.args)

		verify(port).findById(projectId, visibilitySearched = null)
		verify(port).validDateTime(
			projectId,
			dateTime.toZonedDateTime(OffsetTime.MIN),
			dateTime.toZonedDateTime(OffsetTime.MAX),
		)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should disableProjectById call existing project and call port update`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(commonProject()))
		whenever(port.update(any())).thenReturn(Mono.just(commonProject()))

		// Act
		service.disableProjectById(currentUser(), projectId).block()

		// Assert
		verify(port).findById(projectId, visibilitySearched = true)
		verify(port).update(commonProject().apply { visible = false })
	}

	@Test
	fun `Should enableProjectById call existing project and call port update`() {
		// Arrange
		val project = commonProject().apply { visible = false }
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))
		whenever(port.update(any())).thenReturn(Mono.just(project))

		// Act
		service.enableProjectById(currentUser(), projectId).block()

		// Assert
		verify(port).findById(projectId, visibilitySearched = false)
		verify(port).update(commonProject())
	}

	@Test
	fun `Should deleteProjectById call existing project and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(commonProject()))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteProjectById(projectId).block()

		// Assert
		verify(port).findById(projectId, visibilitySearched = null)
		verify(port).deleteById(projectId)
	}

	@Test
	fun `Should purgeProjectsIfNecessary call project eligible for purge, and call port deleteById`() {
		// Arrange
		val date = LocalDate.EPOCH
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()

		whenever(port.findProjectsEligibleForPurge(any())).thenReturn(Flux.just(uuid1, uuid2))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.purgeProjectsIfNecessary(date, false).collectList().block()

		// Assert
		verify(port).findProjectsEligibleForPurge(date)
		verify(port).deleteById(uuid1)
		verify(port).deleteById(uuid2)
	}

	@Test
	fun `Should purgeProjectsIfNecessary call project eligible for purge, and not call port deleteById because of dryRun`() {
		// Arrange
		val date = LocalDate.EPOCH

		whenever(port.findProjectsEligibleForPurge(any())).thenReturn(Flux.just(UUID.randomUUID()))

		// Act
		service.purgeProjectsIfNecessary(date, true).collectList().block()

		// Assert
		verify(port).findProjectsEligibleForPurge(date)
		verify(port, never()).deleteById(any())
	}
}
