package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.repository.IEventModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDateTime
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

class EventModelPostgresRepositoryTest(
    @Autowired private val repository: IEventModelRepository,
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IEventEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: EventEntityMapper

    companion object {
        @JvmStatic
        fun `Should validDateTime call repository validDateTime`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(LocalDateTime.now(), LocalDateTime.now(), false),
                Arguments.of(LocalDateTime.MIN, LocalDateTime.MIN, false),
                Arguments.of(LocalDateTime.MAX, LocalDateTime.MAX, false),
                Arguments.of(null, null, true),
                Arguments.of(LocalDateTime.MIN, LocalDateTime.MAX, true),
            )
        }
    }

    @Test
    fun `Should findPage call repository count and findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = EventSearchParamModel()

        // Act
        val result = repository.findPage(pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findAll(
            textSearched = null,
            visibilitySearched = null,
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAll(
            textSearched = null,
            visibilitySearched = null,
            dateTimeSearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findPage call repository countAllInEventIds and findAllInEventIds`() {
        // Arrange
        val ids = listOf(eventId)
        val pageable = PageableModel(0, 10)
        val params = EventSearchParamModel()

        // Act
        val result = repository.findPage(ids, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findAllInEventIds(
            ids,
            textSearched = null,
            visibilitySearched = null,
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAllInEventIds(
            ids,
            textSearched = null,
            visibilitySearched = null,
            dateTimeSearched = null,
        )
        verify(mapper).toModel(any())
    }

    @ParameterizedTest
    @MethodSource
    fun `Should validDateTime call repository validDateTime`(
        begin: LocalDateTime?,
        end: LocalDateTime?,
        expected: Boolean,
    ) {
        // Act
        val result = repository.validDateTime(eventId, begin, end).block()

        // Assert
        assertEquals(expected, result)
        verify(postgresRepository).validDateTime(eventId, begin, end)
    }

    @Test
    fun `Should findById call repository findById`() {
        // Act
        val result = repository.findById(eventId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findById(
            eventId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findById call repository findById and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findById(uuid, visibilitySearched = null).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findById(
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
            val event = EventModel().apply {
                name = "test"
                create(currentUser())
            }

            // Act
            val result = repository.create(event).block()
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
            val event = EventModel().apply {
                name = "test update"
                create(currentUser())
            }

            // Act
            val result = repository.update(event).block()

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
