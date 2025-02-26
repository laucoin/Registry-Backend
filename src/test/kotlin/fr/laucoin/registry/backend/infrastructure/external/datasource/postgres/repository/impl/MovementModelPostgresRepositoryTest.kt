package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class MovementModelPostgresRepositoryTest(
    @Autowired private val repository: IMovementModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IMovementEntityRepository

    @MockitoSpyBean
    private lateinit var contentPostgresRepository: IMovementContentEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: MovementEntityMapper

    @MockitoSpyBean
    private lateinit var contentMapper: MovementContentEntityMapper

    companion object {
        @JvmStatic
        fun `Should findContent call contentRepository findAllByMovementIds`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    listOf(movementId),
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
        val params = MovementSearchParamModel(typeSearched = null)

        // Act
        val result = repository.findPage(eventId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result !!.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(50, result.totalElements)
        assertEquals(5, result.totalPages)
        verify(postgresRepository).findAll(
            eventId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAll(
            eventId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(mapper, times(10)).toModel(any())
    }

    @Test
    fun `Should findPageByParticipantId call repository countAllByParticipantId and findAllByParticipantId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = MovementSearchParamModel(typeSearched = null)

        // Act
        val result = repository.findPageByParticipantId(eventId, participantId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result !!.pageNumber)
        assertEquals(10, result.pageSize)
        verify(postgresRepository).findAllByParticipantId(
            eventId,
            participantId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAllByParticipantId(
            eventId,
            participantId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(mapper, atLeastOnce()).toModel(any())
    }

    @Test
    fun `Should findPageByVehicleId call repository countAllByVehicleId and findAllByVehicleId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = MovementSearchParamModel(typeSearched = null)

        // Act
        val result = repository.findPageByVehicleId(eventId, vehicleId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result !!.pageNumber)
        assertEquals(10, result.pageSize)
        verify(postgresRepository).findAllByVehicleId(
            eventId,
            vehicleId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAllByVehicleId(
            eventId,
            vehicleId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
    }

    @Test
    fun `Should findPageByActivityId call repository countAllByActivityId and findAllByActivityId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = MovementSearchParamModel(typeSearched = null)

        // Act
        val result = repository.findPageByActivityId(eventId, activityId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result !!.pageNumber)
        assertEquals(10, result.pageSize)
        verify(postgresRepository).findAllByActivityId(
            eventId,
            activityId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAllByActivityId(
            eventId,
            activityId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findContent call contentRepository findAllByMovementIds`(
        ids: List<UUID>,
        expectedContentRepositoryCall: Int,
    ) {
        // Act
        repository.findContent(eventId, ids).collectList().block()

        // Assert
        verify(contentPostgresRepository, times(expectedContentRepositoryCall)).findAllByMovementIds(
            eventId,
            ids,
        )
        verify(contentMapper, atLeast(expectedContentRepositoryCall)).toModel(any())
    }

    @Test
    fun `Should findById call repository findById`() {
        // Act
        val result = repository.findById(eventId, movementId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findById(
            eventId,
            movementId,
            visibilitySearched = null,
        )
        verify(contentPostgresRepository).findAllByMovementIds(
            eventId,
            listOf(movementId),
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should countAllByParticipantId call repository countAllByParticipantId`() {
        // Act
        val result = repository.countAllByParticipantId(eventId, participantId).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).countAllByParticipantId(
            eventId,
            participantId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should countAllByVehicleId call repository countAllByVehicleId`() {
        // Act
        val result = repository.countAllByVehicleId(eventId, vehicleId).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).countAllByVehicleId(
            eventId,
            vehicleId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should countAllByActivityId call repository countAllByActivityId`() {
        // Act
        val result = repository.countAllByActivityId(eventId, activityId).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).countAllByActivityId(
            eventId,
            activityId,
            visibilitySearched = null,
            typeSearched = listOf(IN, OUT),
            startDateTimeSearched = null,
            endDateTimeSearched = null,
        )
        verify(mapper, never()).toModel(any())
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
        verify(contentPostgresRepository).findAllByMovementIds(
            eventId,
            listOf(uuid),
        )
        verify(mapper, never()).toModel(any())
    }

    @Nested
    @TestInstance(PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    inner class WritingTests {
        private val movementDateTime: ZonedDateTime = ZonedDateTime.now()
        private lateinit var uuid: UUID

        @Test
        @Order(1)
        fun `Should create call repository save`() {
            // Arrange
            val movement = MovementModel().apply {
                dateTime = movementDateTime
                type = IN
                event = EventModel().apply { id = eventId }
                create(currentUser())
            }

            // Act
            val result = repository.create(movement).block()
            uuid = result !!.id !!

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(mapper).toEntity(any())
            verify(mapper).toModel(any())
        }

        @Test
        @Order(2)
        fun `Should update call repository save and add member in movement`() {
            // Arrange
            val movement = MovementModel().apply {
                id = uuid
                dateTime = movementDateTime
                type = IN
                event = EventModel().apply { id = eventId }
                content = listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = participantId } })
                create(currentUser())
            }

            // Act
            val result = repository.update(movement).block()

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(postgresRepository).findById(eventId, uuid, visibilitySearched = null)
            verify(contentPostgresRepository).findAllByMovementIds(eventId, listOf(uuid))
            verify(mapper).toEntity(any())
            verify(mapper, times(2)).toModel(any())
        }

        @Test
        @Order(3)
        fun `Should update call repository save and remove member in movement`() {
            // Arrange
            val movement = MovementModel().apply {
                id = uuid
                dateTime = movementDateTime
                type = IN
                event = EventModel().apply { id = eventId }
                content = emptyList()
                create(currentUser())
            }

            // Act
            val result = repository.update(movement).block()

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(postgresRepository).findById(eventId, uuid, visibilitySearched = null)
            verify(contentPostgresRepository).findAllByMovementIds(eventId, listOf(uuid))
            verify(mapper).toEntity(any())
            verify(mapper, times(2)).toModel(any())
        }

        @Test
        @Order(4)
        fun `Should deleteById call repository deleteById`() {
            // Act
            repository.deleteById(uuid).block()

            // Assert
            verify(postgresRepository).deleteById(uuid)
        }
    }
}
