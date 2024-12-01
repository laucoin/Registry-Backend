package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementContentEntityRepository
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MovementContentModelPostgresRepositoryTest {
    private val repository: IMovementContentEntityRepository = mock()
    private val mapper: MovementContentEntityMapper = spy()
    private val modelRepository: MovementContentModelPostgresRepository =
        MovementContentModelPostgresRepository(repository, mapper)

    @Test
    fun `Should saveAll call repository saveAll`() {
        // Arrange
        val movement = MovementContentModel()
        val movementEntity = MovementContentEntity()
        `when`(repository.saveAll(any<List<MovementContentEntity>>())).thenReturn(Flux.just(*arrayOf(movementEntity)))

        // Act
        modelRepository.saveAll(listOf(movement)).blockFirst()

        // Assert
        verify(repository, times(1)).saveAll(any<List<MovementContentEntity>>())
        verify(mapper, times(1)).toEntity(movement)
        verify(mapper, times(1)).toModel(movementEntity)
    }

    @Test
    fun `Should save call repository save`() {
        // Arrange
        val movement = MovementContentModel()
        val movementEntity = MovementContentEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(movementEntity))

        // Act
        modelRepository.save(movement).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(movement)
        verify(mapper, times(1)).toModel(movementEntity)
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
    }
}
