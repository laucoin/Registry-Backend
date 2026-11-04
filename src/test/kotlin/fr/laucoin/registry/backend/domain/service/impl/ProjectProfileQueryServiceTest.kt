package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.IFeatureService
import fr.laucoin.registry.backend.domain.service.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectProfileId
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.util.UUID

class ProjectProfileQueryServiceTest {
	private val port: IProjectProfilePort = mock()
	private val roleService: IRoleService = mock()
	private val userPort: IUserPort = mock()
	private val profileService: IUserProjectProfileService = mock()
	private val principalCache: IPrincipalCacheService = mock()
	private val transactionalOperator: TransactionalOperator = mock()
	private val maxUser: Int = 1
	private val userService: IUserService = mock()
	private val featureService: IFeatureService = mock()
	private val rowLockPort: IRowLockPort = mock()
	private val service = ProjectProfileQueryService(port, userPort, roleService, Clock.systemUTC(), maxUser)



	@Test
	fun `Should findProjectProfilesPage call port findProjectProfilesPageByProjectId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(status = ACCEPTED)
		whenever(port.findProjectProfilesPageByProjectId(any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findProjectProfilesPage(defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findProjectProfilesPageByProjectId(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findProjectProfileById call port findById`() {
		// Arrange
		val profile = ProjectProfileModel().apply {
			role = "PROJECT_ROLE"; this.projectId = defaultProjectId
		}
		val uuid = UUID.randomUUID()
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.just(profile))

		// Act
		service.findProjectProfileById(defaultProjectId, uuid, onlyVisible).block()

		// Assert
		verify(port).findById(defaultProjectId, uuid, onlyVisible)
	}

	@Test
	fun `Should findProjectProfileById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true
		whenever(port.findById(any(), any(), anyOrNull())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findProjectProfileById(defaultProjectId, defaultProjectProfileId, onlyVisible).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultProjectProfileId.toString(), result.args?.first())

		verify(port).findById(defaultProjectId, defaultProjectProfileId, onlyVisible)
	}

	@Test
	fun `Should searchUsers call port findWithLimit`() {
		// Arrange
		val text = "text"
		whenever(userPort.findWithLimit(any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchUsers(text).blockFirst()

		// Assert
		verify(userPort).findWithLimit(eq(maxUser), eq(UserSearchParamModel(text, isVisible = true)))
	}

	@Test
	fun `Should getAssignableProjectRoles call port and role service`() {
		// Arrange
		val profile = ProjectProfileModel().apply {
			role = "PROJECT_ROLE"; this.projectId = defaultProjectId
		}
		whenever(port.findProjectProfileByProjectAndUserId(any(), any(), anyOrNull())).thenReturn(
			Mono.just(
				profile
			)
		)
		whenever(roleService.getAssignableProjectRoles(any())).thenReturn(emptyList())

		// Act
		service.getAssignableProjectRoles(currentUser(), defaultProjectId).blockFirst()

		// Assert
		verify(port).findProjectProfileByProjectAndUserId(
			defaultProjectId,
			currentUser().id!!,
			ProjectProfileSearchParamModel(
				isVisible = true,
				isAvailable = true,
				status = listOf(ACCEPTED),
			),
		)
		verify(roleService).getAssignableProjectRoles(profile)
	}

	@Test
	fun `Should getAssignableProjectRoles throw on port findProjectProfileByProjectAndUserId return empty`() {
		// Arrange
		whenever(port.findProjectProfileByProjectAndUserId(any(), any(), anyOrNull())).thenReturn(Mono.empty())
		whenever(roleService.getAssignableProjectRoles(any())).thenReturn(emptyList())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.getAssignableProjectRoles(currentUser(), defaultProjectId).blockFirst()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		verify(port).findProjectProfileByProjectAndUserId(
			defaultProjectId,
			currentUser().id!!,
			ProjectProfileSearchParamModel(
				isVisible = true,
				isAvailable = true,
				status = listOf(ACCEPTED),
			),
		)
		verify(roleService, never()).getAssignableProjectRoles(any())
	}
}
