package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IUserEntityRepository
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserModelPostgresRepositoryTest {
    private val repository: IUserEntityRepository = mock()
    private val mapper: UserEntityMapper = spy()
    private val currentUserMapper: CurrentUserEntityMapper = spy()
    private val modelRepository: UserModelPostgresRepository = UserModelPostgresRepository(repository, mapper, currentUserMapper)

    @Test
    fun `Should findAll call repository findAll`() {
        // Arrange
        val user = UserEntity()
        val onlyVisible = true
        `when`(repository.findAll(any())).thenReturn(Flux.just(user))

        // Act
        modelRepository.findAll(onlyVisible).blockFirst()

        // Assert
        verify(repository, times(1)).findAll(onlyVisible)
        verify(mapper, times(1)).toModel(user)
        verifyNoInteractions(currentUserMapper)
    }

    @Test
    fun `Should findById call repository findById`() {
        // Arrange
        val user = UserEntity()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(user))

        // Act
        modelRepository.findById(uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible)
        verify(mapper, times(1)).toModel(user)
        verifyNoInteractions(currentUserMapper)
    }

    @Test
    fun `Should findByOidcId call repository findByOidcId`() {
        // Arrange
        val user = CurrentUserEntity()
        val oidcId = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findByOidcId(any(), any())).thenReturn(Mono.just(user))

        // Act
        modelRepository.findByOidcId(oidcId, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findByOidcId(oidcId, onlyVisible)
        verifyNoInteractions(mapper)
        verify(currentUserMapper, times(1)).toModel(user)
    }

    @Test
    fun `Should findServiceAccount call repository findServiceAccount`() {
        // Arrange
        val user = UserEntity()
        `when`(repository.findServiceAccount()).thenReturn(Mono.just(user))

        // Act
        modelRepository.findServiceAccount().block()

        // Assert
        verify(repository, times(1)).findServiceAccount()
        verify(mapper, times(1)).toModel(user)
        verifyNoInteractions(currentUserMapper)
    }

    @Test
    fun `Should findByRoleLevel call repository findByRoleLevel`() {
        // Arrange
        val user = UserEntity()
        val roleLevel = 0
        val onlyVisible = true
        `when`(repository.findByRoleLevel(any(), any())).thenReturn(Flux.just(user))

        // Act
        modelRepository.findByRoleLevel(roleLevel, onlyVisible).blockFirst()

        // Assert
        verify(repository, times(1)).findByRoleLevel(roleLevel, onlyVisible)
        verify(mapper, times(1)).toModel(user)
        verifyNoInteractions(currentUserMapper)
    }

    @Test
    fun `Should save call repository save`() {
        // Arrange
        val user = UserModel()
        val userEntity = UserEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(userEntity))

        // Act
        modelRepository.create(user).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(user)
        verify(mapper, times(1)).toModel(userEntity)
        verifyNoInteractions(currentUserMapper)
    }

    @Test
    fun `Should deleteById call repository deleteById`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.deleteById(any<UUID>())).thenReturn(Mono.empty())

        // Act
        modelRepository.deleteById(uuid).block()

        // Assert
        verify(repository, times(1)).deleteById(uuid)
        verifyNoInteractions(mapper)
        verifyNoInteractions(currentUserMapper)
    }
}
