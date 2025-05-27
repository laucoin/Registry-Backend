package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.repository.ICommunicationModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.CommunicationEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.ICommunicationEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.communicationId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.ZonedDateTime
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
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class CommunicationModelPostgresRepositoryTest(
    @Autowired private val repository: ICommunicationModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: ICommunicationEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: CommunicationEntityMapper

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
        val params = CommunicationSearchParamModel()

        // Act
        val result = repository.findPage(projectId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(550, result.totalElements)
        assertEquals(55, result.totalPages)
        verify(postgresRepository).findAll(
            projectId,
            textSearched = null,
            visibilitySearched = null,
            startDateTimeSearched = null,
            endDateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAll(
            projectId,
            textSearched = null,
            visibilitySearched = null,
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(mapper, atLeastOnce()).toModel(any())
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
    fun `Should findById call repository findById`() {
        // Act
        val result = repository.findById(projectId, communicationId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findById(
            projectId,
            communicationId,
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
            val communication = CommunicationModel().apply {
                dateTime = ZonedDateTime.now()
                message = "message"
                movement = MovementModel().apply { id = movementId }
                project = ProjectModel().apply { id = projectId }
                create(currentUser())
            }

            // Act
            val result = repository.create(communication).block()
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
            val activity = CommunicationModel().apply {
                id = uuid
                dateTime = ZonedDateTime.now()
                message = "message updated"
                movement = MovementModel().apply { id = movementId }
                project = ProjectModel().apply { id = projectId }
                create(currentUser())
            }

            // Act
            val result = repository.update(activity).block()

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
