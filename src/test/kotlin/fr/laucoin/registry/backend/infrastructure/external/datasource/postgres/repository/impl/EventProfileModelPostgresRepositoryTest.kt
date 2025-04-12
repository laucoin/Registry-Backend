package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventProfileEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.eventId
import fr.laucoin.registry.backend.test.ModelExt.eventProfileId
import fr.laucoin.registry.backend.test.ModelExt.userIdWithoutProfile
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class EventProfileModelPostgresRepositoryTest(
    @Autowired private val repository: IEventProfileModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IEventProfileEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: EventProfileEntityMapper

    @MockitoSpyBean
    private lateinit var roleMapper: EventProfileRoleEntityMapper

    @MockitoSpyBean
    private lateinit var roleCountMapper: EventProfileRoleCountEntityMapper

    @Test
    fun `Should findEventProfilesPageByUserId call repository countByUserId and findByUserId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = EventProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findEventProfilesPageByUserId(currentUser().id !!, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findByUserId(
            currentUser().id !!,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countByUserId(
            currentUser().id !!,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            dateTimeSearched = null,
        )
        verify(mapper, times(1)).toModel(any())
    }

    @Test
    fun `Should findEventProfilesPageByEventId call repository countByEventId and findByEventId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = EventProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findEventProfilesPageByEventId(eventId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(2, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findByEventId(
            eventId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countByEventId(
            eventId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            dateTimeSearched = null,
        )
        verify(mapper, times(2)).toModel(any())
    }

    @Test
    fun `Should findUserIdsWithEventProfileForEventWithProfileExclusion call repository findUserIdsWithEventProfileForEventWithProfileExclusion`() {
        // Act
        val result = repository.findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            listOf(currentUser().id !!),
            profileIdToExclude = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            startDateTimeSearched = null,
            endDateTimeSearched = null
        ).collectList().block()

        // Assert
        assertEquals(1, result?.size)
        verify(postgresRepository).findUserIdsWithEventProfileForEventWithProfileExclusion(
            eventId,
            listOf(currentUser().id !!),
            profileIdToExclude = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            startDateTimeSearched = null,
            endDateTimeSearched = null
        )
    }

    @Test
    fun `Should findEventProfilesRolesByUserId call repository findAllRolesByUserId`() {
        // Act
        val result = repository.findEventProfilesRolesByUserId(currentUser().id !!)
            .collectList()
            .block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findAllRolesByUserId(
            currentUser().id !!,
            visibilitySearched = null,
            availabilitySearched = true,
            statusSearched = listOf(ACCEPTED),
        )
        verify(roleMapper).toModel(any())
    }

    @Test
    fun `Should findEventProfileByUserIdAndId call repository findByUserIdAndId`() {
        // Act
        val result = repository.findEventProfileByUserIdAndId(
            currentUser().id !!,
            eventProfileId,
            visibilitySearched = null
        ).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findByUserIdAndId(
            currentUser().id !!,
            eventProfileId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findEventProfileByUserIdAndId call repository findByUserIdAndId and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findEventProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = null).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findByUserIdAndId(
            currentUser().id !!,
            uuid,
            visibilitySearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findById call repository findByEventIdAndId`() {
        // Act
        val result = repository.findById(eventId, eventProfileId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findByEventIdAndId(
            eventId,
            eventProfileId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findById call repository findByEventIdAndId and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findById(eventId, uuid, visibilitySearched = null).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findByEventIdAndId(
            eventId,
            uuid,
            visibilitySearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findEventProfileByEventAndUserId call repository findUsableProfileByEventAndUserId`() {
        // Arrange
        val params = EventProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findEventProfileByEventAndUserId(eventId, currentUser().id !!, params).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findEventProfileByEventAndUserId(
            eventId,
            currentUser().id !!,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findEventProfileByEventAndUserId call repository findUsableProfileByEventAndUserId and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val params = EventProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findEventProfileByEventAndUserId(eventId, uuid, searchParams = params).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findEventProfileByEventAndUserId(
            eventId,
            uuid,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findLevel0EventProfileRoleByUserId call repository findLevel0EventProfileRoleByUserId`() {
        // Act
        val result =
            repository.findLevel0EventProfileRoleByUserId(currentUser().id !!, visibilitySearched = null).collectList().block()

        // Assert
        assertFalse(result.isNullOrEmpty())
        verify(postgresRepository).findLevel0EventProfileRoleByUserId(
            currentUser().id !!,
            visibilitySearched = null,
        )
        verify(roleCountMapper).toModel(any())
    }

    @Test
    fun `Should findLevel0EventProfileRoleByEventId call repository findLevel0EventProfileRoleByEventId`() {
        // Act
        val result =
            repository.findLevel0EventProfileRoleByEventId(eventId, visibilitySearched = null).collectList().block()

        // Assert
        assertFalse(result.isNullOrEmpty())
        verify(postgresRepository).findLevel0EventProfileRoleByEventId(
            eventId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
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
            val eventProfile = EventProfileModel().apply {
                user = UserModel().apply { id = userIdWithoutProfile }
                event = EventModel().apply { id = eventId }
                role = "EVENT_ADMINISTRATOR"
                status = INVITED
                create(currentUser())
            }

            // Act
            val result = repository.create(eventProfile).block()
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
            val eventProfile = EventProfileModel().apply {
                user = UserModel().apply { id = userIdWithoutProfile }
                event = EventModel().apply { id = eventId }
                role = "EVENT_ADMINISTRATOR"
                status = ACCEPTED
                create(currentUser())
            }

            // Act
            val result = repository.update(eventProfile).block()

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

        @Test
        @Order(4)
        fun `Should saveAll call repository saveAll`() {
            // Arrange
            val eventProfile = EventProfileModel().apply {
                user = UserModel().apply { id = userIdWithoutProfile }
                event = EventModel().apply { id = eventId }
                role = "EVENT_ADMINISTRATOR"
                status = INVITED
                create(currentUser())
            }

            // Act
            val result = repository.saveAll(listOf(eventProfile)).collectList().block()

            // Assert
            assertFalse(result.isNullOrEmpty())
            verify(mapper).toEntity(any())
            verify(mapper).toModel(any())
        }
    }
}
