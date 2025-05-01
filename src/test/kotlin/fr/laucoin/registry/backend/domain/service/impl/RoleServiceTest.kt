package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.VEHICLE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.domain.repository.IRoleModelRepository
import fr.laucoin.registry.backend.test.ModelExt.projectId
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.util.ReflectionTestUtils.setField
import reactor.core.publisher.Flux

class RoleServiceTest {
    private val repository: IRoleModelRepository = mock()
    private val defaultRole: String = "ROLE_2"
    private val service: RoleService = RoleService(repository, defaultRole)

    private val roles = hashMapOf(
        "ROLE_0" to Pair(0, listOf("PERMISSION_0")),
        "ROLE_1_1" to Pair(1, listOf("PERMISSION_1")),
        "ROLE_1_2" to Pair(1, listOf("PERMISSION_1")),
        "ROLE_2" to Pair(2, listOf("PERMISSION_2")),
    )

    companion object {
        @JvmStatic
        fun `Should getLevelByUserRole return this right level`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, null),
            Arguments.of("WRONG_ROLE", null),
            Arguments.of("ROLE_0", 0),
            Arguments.of("ROLE_1_1", 1),
            Arguments.of("ROLE_1_2", 1),
            Arguments.of("ROLE_2", 2),
        )

        @JvmStatic
        fun `Should getLevelByProjectRole return this right level`(): Stream<Arguments> = Stream.of(
            Arguments.of("WRONG_ROLE", 0),
            Arguments.of("ROLE_0", 0),
            Arguments.of("ROLE_1_1", 1),
            Arguments.of("ROLE_1_2", 1),
            Arguments.of("ROLE_2", 2),
        )

        @JvmStatic
        fun `Should getAuthoritiesByUserRole return a list of associated authorities`(): Stream<Arguments> = Stream.of(
            Arguments.of("WRONG_ROLE", emptyList<String>()),
            Arguments.of("ROLE_0", listOf("PERMISSION_0")),
            Arguments.of("ROLE_1_1", listOf("PERMISSION_1")),
            Arguments.of("ROLE_1_2", listOf("PERMISSION_1")),
            Arguments.of("ROLE_2", listOf("PERMISSION_2")),
        )

        @JvmStatic
        fun `Should getAuthoritiesByProjectRole return a list of associated authorities`(): Stream<Arguments> = Stream.of(
            Arguments.of("WRONG_ROLE", emptyList<String>()),
            Arguments.of("ROLE_0", listOf("${projectId}_PERMISSION_0")),
            Arguments.of("ROLE_1_1", listOf("${projectId}_PERMISSION_1")),
            Arguments.of("ROLE_1_2", listOf("${projectId}_PERMISSION_1")),
            Arguments.of("ROLE_2", listOf("${projectId}_PERMISSION_2")),
        )

        @JvmStatic
        fun `Assignable roles`(): Stream<Arguments> = Stream.of(
            Arguments.of("ROLE_0", listOf("ROLE_0", "ROLE_1_1", "ROLE_1_2", "ROLE_2")),
            Arguments.of("ROLE_1_1", listOf("ROLE_1_1", "ROLE_2")),
            Arguments.of("ROLE_1_2", listOf("ROLE_1_2", "ROLE_2")),
            Arguments.of("ROLE_2", listOf("ROLE_2")),
            Arguments.of("WRONG_ROLE", emptyList<String>()),
        )
    }

    @Test
    fun `Should onApplicationProject call user and project role`() {
        // Arrange
        val event: ContextRefreshedEvent = mock()
        val userRole = RoleModel(
            role = "USER_ROLE_0",
            level = 0,
            permissions = listOf("USER_PERMISSION_0"),
        )
        whenever(repository.findUserRoles()).thenReturn(Flux.just(userRole))
        val projectRole = RoleModel(
            role = "PROJECT_ROLE_0",
            level = 0,
            permissions = listOf("PROJECT_PERMISSION_0"),
        )
        whenever(repository.findProjectRoles()).thenReturn(Flux.just(projectRole))

        // Act
        service.onApplicationEvent(event)

        // Assert
        verify(repository).findUserRoles()
        verify(repository).findProjectRoles()
    }

    @ParameterizedTest
    @MethodSource
    fun `Should getLevelByUserRole return this right level`(
        role: String?,
        expected: Int?,
    ) {
        // Arrange
        setField(service, "userRoles", roles)

        // Act
        val result = service.getLevelByUserRole(role)

        // Assert
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should getLevelByProjectRole return this right level`(
        role: String,
        expected: Int?,
    ) {
        // Arrange
        setField(service, "projectRoles", roles)

        // Act
        val result = service.getLevelByProjectRole(role)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `Should getLevel0RoleFromProjectRoles return this right role`() {
        // Arrange
        setField(service, "projectRoles", roles)

        // Act
        val result = service.getLevel0RoleFromProjectRoles()

        // Assert
        assertEquals("ROLE_0", result)
    }

    @Test
    fun `Should getDefaultUserRole filter user role on defaultRole value`() {
        // Arrange
        setField(service, "userRoles", roles)

        // Act
        val result = service.getDefaultUserRole()

        // Assert
        assertEquals(defaultRole, result)
    }

    @Test
    fun `Should getDefaultUserRole return null on null role`() {
        // Act
        val result = service.getDefaultUserRole()

        // Assert
        assertNull(result)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should getAuthoritiesByUserRole return a list of associated authorities`(
        role: String?,
        expectedAuthorities: List<String>,
    ) {
        // Arrange
        setField(service, "userRoles", roles)

        // Act
        val result = service.getAuthoritiesByUserRole(role)

        // Assert
        assertEquals(expectedAuthorities, result)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should getAuthoritiesByProjectRole return a list of associated authorities`(
        role: String,
        expectedAuthorities: List<String>,
    ) {
        // Arrange
        setField(service, "projectRoles", roles)

        // Act
        val result = service.getAuthoritiesByProjectRole(role, projectId, visibility = true)

        // Assert
        assertEquals(expectedAuthorities, result)
    }

    @Test
    fun `Should getOptionAuthoritiesByProject return formatted authorities for project option`() {
        // Arrange
        val options = listOf(VEHICLE, ACTIVITY)

        // Act
        val result = service.getOptionAuthoritiesByProject(projectId, options)

        // Assert
        assertEquals(2, result.size)
        assertEquals("${projectId}_${REGISTRY_PROJECT_OPTION_PREFIX}${VEHICLE}", result.first())
        assertEquals("${projectId}_${REGISTRY_PROJECT_OPTION_PREFIX}${ACTIVITY}", result.last())
    }

    @Test
    fun `Should getProjectIdsFromCurrentUserProfiles return formatted authorities for project option`() {
        // Arrange
        val authorities: MutableList<GrantedAuthority> = mutableListOf(
            SimpleGrantedAuthority("ROLE_0"),
            SimpleGrantedAuthority("ROLE_1"),
            SimpleGrantedAuthority("${projectId}_${REGISTRY_PROJECT_OPTION_PREFIX}${VEHICLE}"),
            SimpleGrantedAuthority("${projectId}_${REGISTRY_PROJECT_OPTION_PREFIX}${ACTIVITY}"),
        )
        val currentUser: CurrentUserModel = mock()
        whenever(currentUser.authorities).thenReturn(authorities)

        // Act
        val result = service.getProjectIdsFromCurrentUserProfiles(currentUser)

        // Assert
        assertEquals(1, result.size)
        assertEquals(projectId, result.first())
    }

    @ParameterizedTest
    @MethodSource("Assignable roles")
    fun `Should getAssignableUserRoles return a list of assignable roles`(
        currentUserRole: String?,
        expectedAssignableRoles: List<String>,
    ) {
        // Arrange
        setField(service, "userRoles", roles)
        val currentUser = CurrentUserModel().apply { role = currentUserRole }

        // Act
        val result = service.getAssignableUserRoles(currentUser)

        // Assert
        assertEquals(expectedAssignableRoles.size, result.size)
        expectedAssignableRoles.forEach {
            assertTrue(result.contains(it))
        }
    }

    @ParameterizedTest
    @MethodSource("Assignable roles")
    fun `Should getAssignableProjectRoles return a list of assignable roles`(
        projectProfileRole: String?,
        expectedAssignableRoles: List<String>,
    ) {
        // Arrange
        setField(service, "projectRoles", roles)
        val projectProfile = ProjectProfileModel().apply { role = projectProfileRole }

        // Act
        val result = service.getAssignableProjectRoles(projectProfile)

        // Assert
        assertEquals(expectedAssignableRoles.size, result.size)
        expectedAssignableRoles.forEach {
            assertTrue(result.contains(it))
        }
    }
}
