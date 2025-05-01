package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.RoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IRoleEntityRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux

class RoleModelPostgresRepositoryTest {
    private val repository: IRoleEntityRepository = mock()
    private val mapper: RoleEntityMapper = spy()
    private val modelRepository: RoleModelPostgresRepository =
        RoleModelPostgresRepository(repository, mapper)

    @Test
    fun `Should findUserRoles call repository findByUserId`() {
        // Arrange
        val role = RoleEntity(role = "ROLE_USER", level = 0, permissions = emptyList())
        whenever(repository.findUserRoles()).thenReturn(Flux.just(role))

        // Act
        modelRepository.findUserRoles().blockFirst()

        // Assert
        verify(repository).findUserRoles()
        verify(mapper).toModel(role)
    }

    @Test
    fun `Should findProjectRoles call repository findProjectRoles`() {
        // Arrange
        val role = RoleEntity(role = "ROLE_PROJECT", level = 0, permissions = emptyList())
        whenever(repository.findProjectRoles()).thenReturn(Flux.just(role))

        // Act
        modelRepository.findProjectRoles().blockFirst()

        // Assert
        verify(repository).findProjectRoles()
        verify(mapper).toModel(role)
    }
}
