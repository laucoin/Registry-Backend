package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.VehicleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IVehicleEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class VehicleModelPostgresRepositoryTest(
    @Autowired private val repository: IVehicleModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IVehicleEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: VehicleEntityMapper

    companion object {
        @JvmStatic
        fun `Should findAllByIds call repository findAllByIds`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    listOf(UUID.randomUUID(), UUID.randomUUID()),
                    1,
                ),
                Arguments.of(
                    emptyList<UUID>(),
                    0,
                ),
            )
        }
    }

    @Test
    fun `Should findPage call repository count and findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = VehicleSearchParamModel()

        // Act
        val result = repository.findPage(projectId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(5, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findAll(
            projectId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            presenceSearched = null,
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAll(
            projectId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            presenceSearched = null,
            dateTimeSearched = null,
        )
        verify(mapper, times(5)).toModel(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAllByIds call repository findAllByIds`(
        ids: List<UUID>,
        expectedDatabaseCall: Int,
    ) {
        // Act
        val result = repository.findAllByIds(projectId, ids, visibilitySearched = null).collectList().block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository, times(expectedDatabaseCall)).findAllByIds(
            projectId,
            ids,
            visibilitySearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findWithLimit call repository findWithLimit`() {
        // Arrange
        val size = 10
        val params = VehicleSearchParamModel()

        // Act
        val result = repository.findWithLimit(size, projectId, params).collectList().block()

        // Assert
        assertNotNull(result)
        assertEquals(5, result.size)
        verify(postgresRepository).findWithLimit(
            projectId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            presenceSearched = null,
            dateTimeSearched = null,
            size,
        )
        verify(mapper, times(5)).toModel(any())
    }

    @Test
    fun `Should findById call repository findById`() {
        // Act
        val result = repository.findById(projectId, vehicleId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findById(
            projectId,
            vehicleId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findById call repository findById and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findById(projectId, uuid, visibilitySearched = null).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findById(
            projectId,
            uuid,
            visibilitySearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Nested
    @TestInstance(PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    inner class WritingTests {
        private lateinit var uuid: UUID

        @Test
        @Order(1)
        fun `Should create call repository save`() {
            // Arrange
            val vehicle = VehicleModel().apply {
                licensePlate = "AB-123-CD"
                brand = "test"
                model = "test"
                project = ProjectModel().apply { id = projectId }
                create(currentUser())
            }

            // Act
            val result = repository.create(vehicle).block()
            uuid = result !!.id !!

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(mapper).toEntity(any())
            verify(mapper).toModel(any())
        }

        @Test
        @Order(2)
        fun `Should update call repository save`() {
            // Arrange
            val vehicle = VehicleModel().apply {
                id = uuid
                licensePlate = "AB-123-CD"
                brand = "test update"
                model = "test update"
                project = ProjectModel().apply { id = projectId }
                create(currentUser())
            }

            // Act
            val result = repository.update(vehicle).block()

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(mapper).toEntity(any())
            verify(mapper).toModel(any())
        }

        @Test
        @Order(3)
        fun `Should deleteById call repository deleteById`() {
            // Act
            repository.deleteById(uuid).block()

            // Assert
            verify(postgresRepository).deleteById(uuid)
        }
    }
}
