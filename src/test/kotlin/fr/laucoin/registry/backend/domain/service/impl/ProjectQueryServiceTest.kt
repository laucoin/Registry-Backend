package fr.laucoin.registry.backend.domain.service.impl

import org.junit.jupiter.api.BeforeEach
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.test.ModelExt
import fr.laucoin.registry.backend.test.ModelExt.defaultProject
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.util.UUID

import fr.laucoin.registry.backend.domain.service.IProjectQueryService

class ProjectQueryServiceTest {
	private val port: IProjectPort = mock()
	private val profilePort: IProjectProfilePort = mock()
	private val favoritePort: IProjectFavoritePort = mock()
	private val roleService: IRoleService = mock()
	private val service: IProjectQueryService =
		ProjectQueryService(port, profilePort, favoritePort, roleService, Clock.systemUTC())

	@BeforeEach
	fun setup() {
		whenever(profilePort.findProjectProfilesByUserIdAndProjectIds(any(), any(), any())).thenReturn(Flux.empty())
		whenever(favoritePort.findFavoriteProjectIds(any(), any())).thenReturn(Flux.empty())
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
	fun `Should findProjectsPage only reveal hidden projects the user can delete`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val params = ProjectSearchParamModel()
		whenever(port.findPage(any(), any())).thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findProjectsPage(
			currentUser(REGISTRY_PROJECT_R, "${projectId}_REGISTRY_PROJECT_RESTORE"),
			PageableModel(0, 10),
			false,
			params,
		).block()

		// Assert
		assertEquals(listOf(projectId), params.hiddenReadableIds)
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
	fun `Should findProjectsPage restrict to the projects of the user profiles by default even with read all`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectSearchParamModel()

		whenever(roleService.getAuthoritiesByUserRole(anyOrNull())).thenReturn(listOf(REGISTRY_PROJECT_R))
		whenever(roleService.getProjectIdsFromCurrentUserProfiles(any())).thenReturn(emptyList())
		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findProjectsPage(currentUser(REGISTRY_PROJECT_R), pageable, null, params).block()

		// Assert
		verify(port).findPage(emptyList(), pageable, params)
		verify(port, never()).findPage(any(), any())
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
		verifyNoInteractions(roleService)
	}
}
