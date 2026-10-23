package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.test.ModelExt
import fr.laucoin.registry.backend.test.ModelExt.defaultProject
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectProfile
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

class ProjectServiceTest {
	private val port: IProjectPort = mock()
	private val profilePort: IProjectProfilePort = mock()
	private val favoritePort: IProjectFavoritePort = mock()
	private val projectProfileService: IUserProjectProfileService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val roleService: IRoleService = mock()
	private val principalCache: IPrincipalCacheService = mock()
	private val service: IProjectService = ProjectService(
		port,
		profilePort,
		favoritePort,
		projectProfileService,
		transactionalOperator,
		roleService,
		principalCache,
	)

	@BeforeEach
	fun setup() {
		whenever(profilePort.findOidcIdsByProjectId(any())).thenReturn(Flux.empty())
		whenever(profilePort.findProjectProfilesByUserIdAndProjectIds(any(), any(), any())).thenReturn(Flux.empty())
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.empty())
	}

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
	fun `Should findProjectsPage refuse to list every project without the global read authority`() {
		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findProjectsPage(currentUser(), PageableModel(0, 10), false, ProjectSearchParamModel()).block()
		}) as RegistryException

		// Assert
		assertEquals(FORBIDDEN, result.status)
		assertEquals(NOT_ENOUGH_PERMISSION, result.code)
		verifyNoInteractions(port)
	}

	@Test
	fun `Should findProjectsPage restrict to the user profiles by default without the global read authority`() {
		// Arrange
		val params = ProjectSearchParamModel()
		whenever(roleService.getProjectIdsFromCurrentUserProfiles(any())).thenReturn(listOf(defaultProjectId))
		whenever(port.findPage(any<List<UUID>>(), any(), any()))
			.thenReturn(Mono.just(PageModel(PageableModel(0, 10), totalElements = 0, emptyList())))
		whenever(profilePort.findProjectProfilesByUserIdAndProjectIds(any(), any(), any())).thenReturn(Flux.empty())
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.empty())

		// Act
		service.findProjectsPage(currentUser(), PageableModel(0, 10), null, params).block()

		// Assert
		verify(port).findPage(eq(listOf(defaultProjectId)), any(), eq(params))
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
	fun `Should findProjectWithProfileById attach the current user profile`() {
		// Arrange
		val profile = ProjectProfileModel().apply { id = UUID.randomUUID(); this.projectId = ModelExt.defaultProjectId }
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))
		whenever(profilePort.findProjectProfilesByUserIdAndProjectIds(any(), any(), any()))
			.thenReturn(Flux.just(profile))

		// Act
		val result = service.findProjectWithProfileById(currentUser(), defaultProjectId).block()

		// Assert
		assertEquals(profile, result?.profile)
		verify(profilePort).findProjectProfilesByUserIdAndProjectIds(
			eq(currentUser().id!!),
			eq(listOf(defaultProjectId)),
			any(),
		)
	}

	@Test
	fun `Should findProjectWithProfileById attach the profile that grants access when the user has several`() {
		// Arrange
		val blocked = ProjectProfileModel().apply { id = UUID.randomUUID(); visible = false; status = ACCEPTED }
		val rejected = ProjectProfileModel().apply { id = UUID.randomUUID(); status = REJECTED }
		val active = ProjectProfileModel().apply {
			id = UUID.randomUUID(); status = ACCEPTED; availabilityStatus = AVAILABLE
		}
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))
		whenever(profilePort.findProjectProfilesByUserIdAndProjectIds(any(), any(), any()))
			.thenReturn(Flux.just(blocked, active, rejected))

		// Act
		val result = service.findProjectWithProfileById(currentUser(), defaultProjectId).block()

		// Assert
		assertEquals(active, result?.profile)
	}

	@Test
	fun `Should findProjectWithProfileById return the project without profile when the user has none`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))

		// Act
		val result = service.findProjectWithProfileById(currentUser(), defaultProjectId).block()

		// Assert
		assertEquals(defaultProject().id, result?.id)
		assertEquals(null, result?.profile)
	}

	@Test
	fun `Should findProjectsPage attach each current user profile to its project`() {
		// Arrange
		val withProfile = ProjectModel().apply { id = UUID.randomUUID() }
		val withoutProfile = ProjectModel().apply { id = UUID.randomUUID() }
		val profile = ProjectProfileModel().apply { id = UUID.randomUUID(); projectId = withProfile.id }
		whenever(port.findPage(any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, listOf(withProfile, withoutProfile))))
		whenever(profilePort.findProjectProfilesByUserIdAndProjectIds(any(), any(), any()))
			.thenReturn(Flux.just(profile))

		// Act
		val result = service.findProjectsPage(
			currentUser(REGISTRY_PROJECT_R),
			PageableModel(0, 10),
			withProfile = false,
			ProjectSearchParamModel(),
		).block()

		// Assert
		assertEquals(profile, result?.content?.first { it.id == withProfile.id }?.profile)
		assertEquals(null, result?.content?.first { it.id == withoutProfile.id }?.profile)
		verify(profilePort).findProjectProfilesByUserIdAndProjectIds(
			eq(currentUser().id!!),
			eq(listOf(withProfile.id!!, withoutProfile.id!!)),
			any(),
		)
	}

	@Test
	fun `Should findProjectsPage flag the favorite projects of the current user`() {
		// Arrange
		val favorite = ProjectModel().apply { id = UUID.randomUUID() }
		val other = ProjectModel().apply { id = UUID.randomUUID() }
		whenever(port.findPage(any(), any())).thenReturn(Mono.just(PageModel(1, 2, 3, 4, listOf(favorite, other))))
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.just(favorite.id!!))

		// Act
		val result = service.findProjectsPage(
			currentUser(REGISTRY_PROJECT_R),
			PageableModel(0, 10),
			withProfile = false,
			ProjectSearchParamModel(),
		).block()

		// Assert
		assertEquals(true, result?.content?.first { it.id == favorite.id }?.favorite)
		assertEquals(false, result?.content?.first { it.id == other.id }?.favorite)
		verify(favoritePort).findFavoriteProjectIds(currentUser().id!!, listOf(favorite.id!!, other.id!!))
	}

	@Test
	fun `Should findProjectWithProfileById flag the project as favorite`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.just(defaultProjectId))

		// Act
		val result = service.findProjectWithProfileById(currentUser(), defaultProjectId).block()

		// Assert
		assertEquals(true, result?.favorite)
		verify(favoritePort).findFavoriteProjectIds(currentUser().id!!, listOf(defaultProjectId))
	}

	@Test
	fun `Should findProjectWithProfileById leave the project as not favorite`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))

		// Act
		val result = service.findProjectWithProfileById(currentUser(), defaultProjectId).block()

		// Assert
		assertEquals(false, result?.favorite)
	}

	@Test
	fun `Should addFavoriteProject call port add once the project exists`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))
		whenever(favoritePort.add(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		service.addFavoriteProject(currentUser(), defaultProjectId).block()

		// Assert
		verify(port).findById(defaultProjectId, isVisible = null)
		verify(favoritePort).add(currentUser().id!!, defaultProjectId)
	}

	@Test
	fun `Should addFavoriteProject throw and not call port add when the project does not exist`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.addFavoriteProject(currentUser(), defaultProjectId).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		verify(favoritePort, never()).add(any(), any())
	}

	@Test
	fun `Should removeFavoriteProject call port remove`() {
		// Arrange
		whenever(favoritePort.remove(any(), any())).thenReturn(Mono.just(Unit))

		// Act
		service.removeFavoriteProject(currentUser(), defaultProjectId).block()

		// Assert
		verify(favoritePort).remove(currentUser().id!!, defaultProjectId)
	}

	@Test
	fun `Should findProjectsPage flag the favorites together with the profiles when listing the user projects`() {
		// Arrange
		val favorite = ProjectModel().apply { id = UUID.randomUUID() }
		whenever(roleService.getProjectIdsFromCurrentUserProfiles(any())).thenReturn(listOf(favorite.id!!))
		whenever(port.findPage(any(), any(), any())).thenReturn(Mono.just(PageModel(1, 2, 3, 4, listOf(favorite))))
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.just(favorite.id!!))

		// Act
		val result =
			service.findProjectsPage(currentUser(), PageableModel(0, 10), true, ProjectSearchParamModel()).block()

		// Assert
		assertEquals(true, result?.content?.single()?.favorite)
	}

	@Test
	fun `Should findProjectsPage return an empty page when there is no project`() {
		// Arrange
		whenever(port.findPage(any(), any())).thenReturn(Mono.just(PageModel(0, 10, 0, 0, emptyList())))

		// Act
		val result = service.findProjectsPage(
			currentUser(REGISTRY_PROJECT_R),
			PageableModel(0, 10),
			withProfile = false,
			ProjectSearchParamModel(),
		).block()

		// Assert
		assertEquals(emptyList<ProjectModel>(), result?.content)
	}

	@Test
	fun `Should enableProjectById flag the project as favorite`() {
		// Arrange
		val project = defaultProject().apply { visible = false }
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))
		whenever(port.update(any())).thenReturn(Mono.just(project))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.just(defaultProjectId))

		// Act
		val result = service.enableProjectById(currentUser(), defaultProjectId).block()

		// Assert
		assertEquals(true, result?.favorite)
	}

	@Test
	fun `Should findProjectsPage search the favorites of the current user`() {
		// Arrange
		val params = ProjectSearchParamModel(query = null, isFavorite = true)
		whenever(port.findPage(any(), any())).thenReturn(Mono.just(PageModel(0, 10, 0, 0, emptyList())))

		// Act
		service.findProjectsPage(currentUser(REGISTRY_PROJECT_R), PageableModel(0, 10), false, params).block()

		// Assert
		assertEquals(currentUser().id, params.userId)
		assertEquals(true, params.isFavorite)
	}

	@Test
	fun `Should findProjectsPage replace a forged userId by the principal id for the port`() {
		// Arrange
		val forged = UUID.randomUUID()
		val params = ProjectSearchParamModel(query = null, isFavorite = true).apply { userId = forged }
		val captor = argumentCaptor<ProjectSearchParamModel>()
		whenever(roleService.getProjectIdsFromCurrentUserProfiles(any())).thenReturn(emptyList())
		whenever(port.findPage(any(), any(), any())).thenReturn(Mono.just(PageModel(0, 10, 0, 0, emptyList())))

		// Act
		service.findProjectsPage(currentUser(), PageableModel(0, 10), true, params).block()

		// Assert
		verify(port).findPage(any(), any(), captor.capture())
		assertEquals(currentUser().id, captor.firstValue.userId)
	}

	@Test
	fun `Should findProjectById call port findById`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))

		// Act
		service.findProjectById(defaultProjectId, onlyVisible).block()

		// Assert
		verify(port).findById(defaultProjectId, onlyVisible)
	}

	@Test
	fun `Should findProjectById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findProjectById(defaultProjectId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultProjectId.toString(), result.args?.first())

		assertEquals(defaultProject().id.toString(), result.args?.first())

		verify(port).findById(defaultProjectId, onlyVisible)
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
		val project = defaultProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = service.validateDateTime(defaultProjectId, dateTime?.copy(), errorMessage).block()

		// Assert
		assertEquals(defaultProjectId, result)

		verify(port).findById(defaultProjectId, isVisible = null)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should validateDateTime call port findById and throw on invalid date`(
		projectBeginDateTime: CustomDateTimeModel?,
		projectEndDateTime: CustomDateTimeModel?,
		dateTime: CustomDateTimeModel?,
	) {
		// Arrange
		val project = defaultProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.validateDateTime(defaultProjectId, dateTime?.copy(), errorMessage).block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(errorMessage, result.message)
		assertEquals(3, result.args?.size)

		verify(port).findById(defaultProjectId, isVisible = null)
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
		val project = defaultProject().apply { begin = projectBeginDateTime; end = projectEndDateTime }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = service.validateDateTimes(defaultProjectId, startDateTime, endDateTime, errorMessage).block()

		// Assert
		assertEquals(defaultProjectId, result)

		verify(port).findById(defaultProjectId, isVisible = null)
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
		val project = defaultProject().apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val errorMessage = "ERROR_MESSAGE"

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.validateDateTimes(defaultProjectId, startDateTime?.copy(), endDateTime?.copy(), errorMessage)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(UNPROCESSABLE_CONTENT, result.status)
		assertEquals(errorMessage, result.message)
		assertEquals(4, result.args?.size)

		verify(port).findById(defaultProjectId, isVisible = null)
	}

	@Test
	fun `Should createProject call profile service createUserProjectProfileFromProject and port create`() {
		// Arrange
		whenever(projectProfileService.createUserProjectProfileFromProject(any(), any()))
			.thenReturn(Mono.just(defaultProjectProfile()))
		whenever(port.create(any())).thenReturn(Mono.just(defaultProject()))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<String>(0) }

		// Act
		service.createProject(currentUser(), defaultProject()).block()

		// Assert
		verify(projectProfileService).createUserProjectProfileFromProject(currentUser(), defaultProject())
		verify(port).create(defaultProject())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should createProject invalidate the cached principal of the creator so the new profile grants its authorities`() {
		// Arrange
		whenever(projectProfileService.createUserProjectProfileFromProject(any(), any()))
			.thenReturn(Mono.just(defaultProjectProfile()))
		whenever(port.create(any())).thenReturn(Mono.just(defaultProject()))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		service.createProject(currentUser(), defaultProject()).block()

		// Assert
		verify(principalCache).invalidate(currentUser().oidcId)
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
		val projectToUpdate = defaultProject()
			.apply { begin = projectBeginDateTime?.copy(); end = projectEndDateTime?.copy() }
		val projectUpdated = defaultProject()
			.apply { begin = newProjectBeginDateTime?.copy(); end = newProjectEndDateTime?.copy() }

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(projectToUpdate))
		whenever(port.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(true))
		whenever(port.update(any())).thenReturn(Mono.just(projectUpdated))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		service.updateProjectById(currentUser(), defaultProjectId, projectUpdated).block()

		// Assert
		verify(port).findById(defaultProjectId, isVisible = null)
		verify(port, times(expectedVerificationCall))
			.validDateTime(eq(defaultProjectId), anyOrNull(), anyOrNull())
		verify(port).update(any())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should updateProjectById call port findById, validDateTime and throw due to date conflict`() {
		// Arrange
		val dateTime = CustomDateTimeModel(LocalDate.EPOCH)
		val projectUpdated = defaultProject().apply { begin = dateTime; end = dateTime }

		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))
		whenever(port.validDateTime(any(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(false))
		whenever(port.update(any())).thenReturn(Mono.just(projectUpdated))

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.updateProjectById(currentUser(), defaultProjectId, projectUpdated).block()
		}) as RegistryException

		// Assert
		assertEquals(CONFLICT, result.status)
		assertEquals(PROJECT_DATE_CONFLICT_WITH_ELEMENTS, result.message)
		assertEquals(arrayListOf(defaultProject().name), result.args)

		verify(port).findById(defaultProjectId, isVisible = null)
		verify(port).validDateTime(
			defaultProjectId,
			dateTime.toZonedDateTime(OffsetTime.MIN),
			dateTime.toZonedDateTime(OffsetTime.MAX),
		)
		verify(port, never()).update(any())
	}

	@Test
	fun `Should disableProjectById call existing project and call port update`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))
		whenever(port.update(any())).thenReturn(Mono.just(defaultProject()))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		service.disableProjectById(currentUser(), defaultProjectId).block()

		// Assert
		verify(port).findById(defaultProjectId, isVisible = true)
		verify(port).update(defaultProject().apply { visible = false })
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should enableProjectById call existing project and call port update`() {
		// Arrange
		val project = defaultProject().apply { visible = false }
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(project))
		whenever(port.update(any())).thenReturn(Mono.just(project))
		whenever(transactionalOperator.transactional(any<Mono<*>>())).thenAnswer { it.getArgument<Mono<*>>(0) }

		// Act
		service.enableProjectById(currentUser(), defaultProjectId).block()

		// Assert
		verify(port).findById(defaultProjectId, isVisible = false)
		verify(port).update(defaultProject())
		verify(transactionalOperator).transactional(any<Mono<*>>())
	}

	@Test
	fun `Should deleteProjectById call existing project and call port deleteById`() {
		// Arrange
		whenever(port.findById(any(), anyOrNull())).thenReturn(Mono.just(defaultProject()))
		whenever(port.deleteById(any())).thenReturn(Mono.empty())

		// Act
		service.deleteProjectById(defaultProjectId).block()

		// Assert
		verify(port).findById(defaultProjectId, isVisible = null)
		verify(port).deleteById(defaultProjectId)
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
