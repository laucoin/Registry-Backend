package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.RoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IRoleEntityRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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
        `when`(repository.findUserRoles()).thenReturn(Flux.just(role))

        // Act
        modelRepository.findUserRoles().blockFirst()

        // Assert
        verify(repository, times(1)).findUserRoles()
        verify(mapper, times(1)).toModel(role)
    }

    @Test
    fun `Should findEventRoles call repository findEventRoles`() {
        // Arrange
        val role = RoleEntity(role = "ROLE_EVENT", level = 0, permissions = emptyList())
        `when`(repository.findEventRoles()).thenReturn(Flux.just(role))

        // Act
        modelRepository.findEventRoles().blockFirst()

        // Assert
        verify(repository, times(1)).findEventRoles()
        verify(mapper, times(1)).toModel(role)
    }
}
