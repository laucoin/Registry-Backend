package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.repository.IActivityModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IActivityEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.eventId
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

class ActivityModelPostgresRepositoryTest(
    @Autowired private val repository: IActivityModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IActivityEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: ActivityEntityMapper

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
        val params = ActivitySearchParamModel()

        // Act
        val result = repository.findPage(eventId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(5, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findAll(
            eventId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAll(
            eventId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
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
        val result = repository.findAllByIds(eventId, ids, visibilitySearched = null).collectList().block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository, times(expectedDatabaseCall)).findAllByIds(
            eventId,
            ids,
            visibilitySearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findWithLimit call repository findWithLimit`() {
        // Arrange
        val size = 10
        val params = ActivitySearchParamModel()

        // Act
        val result = repository.findWithLimit(size, eventId, params).collectList().block()

        // Assert
        assertNotNull(result)
        assertEquals(5, result.size)
        verify(postgresRepository).findWithLimit(
            eventId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            dateTimeSearched = null,
            size,
        )
        verify(mapper, times(5)).toModel(any())
    }

    @Test
    fun `Should findById call repository findById`() {
        // Act
        val result = repository.findById(eventId, activityId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findById(
            eventId,
            activityId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findById call repository findById and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findById(eventId, uuid, visibilitySearched = null).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findById(
            eventId,
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
            val activity = ActivityModel().apply {
                name = "test"
                description = "test"
                event = EventModel().apply { id = eventId }
                create(currentUser())
            }

            // Act
            val result = repository.create(activity).block()
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
            val activity = ActivityModel().apply {
                id = uuid
                name = "test update"
                description = "test update"
                event = EventModel().apply { id = eventId }
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
