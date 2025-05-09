package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IParticipantEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.groupId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
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

class ParticipantModelPostgresRepositoryTest(
    @Autowired private val repository: IParticipantModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IParticipantEntityRepository

    @MockitoSpyBean
    private lateinit var contentPostgresRepository: IGroupContentEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: ParticipantEntityMapper

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
        val params = ParticipantSearchParamModel()

        // Act
        val result = repository.findPage(projectId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(50, result.totalElements)
        assertEquals(5, result.totalPages)
        verify(postgresRepository).findAll(
            projectId,
            textSearched = null,
            isMajor = null,
            typeSearched = null,
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
            isMajor = null,
            typeSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            presenceSearched = null,
            dateTimeSearched = null,
        )
        verify(mapper, times(10)).toModel(any())
    }

    @Test
    fun `Should findPageByGroupId call repository countAllByGroupId and findAllByGroupId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = ParticipantSearchParamModel()

        // Act
        val result = repository.findPageByGroupId(projectId, groupId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(3, result.totalPages)
        verify(postgresRepository).findAllByGroupId(
            projectId,
            groupId,
            textSearched = null,
            isMajor = null,
            typeSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            presenceSearched = null,
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAllByGroupId(
            projectId,
            groupId,
            textSearched = null,
            isMajor = null,
            typeSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            presenceSearched = null,
            dateTimeSearched = null,
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
            dateTimeSearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findByUserId call repository findByUserId`() {
        // Act
        repository.findByUserId(projectId, currentUser().id !!).collectList().block()

        // Assert
        verify(postgresRepository).findByUserId(projectId, currentUser().id !!, dateTimeSearched = null)
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findWithLimit call repository findWithLimit`() {
        // Arrange
        val size = 10
        val params = ParticipantSearchParamModel()

        // Act
        val result = repository.findWithLimit(size, projectId, params).collectList().block()

        // Assert
        assertNotNull(result)
        assertEquals(10, result.size)
        verify(postgresRepository).findWithLimit(
            projectId,
            textSearched = null,
            isMajor = null,
            typeSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            presenceSearched = null,
            dateTimeSearched = null,
            size,
        )
        verify(mapper, times(10)).toModel(any())
    }

    @Test
    fun `Should findById call repository findById`() {
        // Act
        val result = repository.findById(projectId, participantId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findById(
            projectId,
            participantId,
            visibilitySearched = null,
            dateTimeSearched = null,
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
            dateTimeSearched = null,
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
            val participant = ParticipantModel().apply {
                firstName = "test"
                lastName = "test"
                birthday = LocalDate.EPOCH
                type = REGISTERED
                project = ProjectModel().apply { id = projectId }
                create(currentUser())
            }

            // Act
            val result = repository.create(participant).block()
            uuid = result !!.id !!

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(mapper).toEntity(any())
            verify(mapper).toModel(any())
        }

        @Test
        @Order(2)
        fun `Should update call repository save and add group for participant`() {
            // Arrange
            val participant = ParticipantModel().apply {
                id = uuid
                firstName = "test updated"
                lastName = "test updated"
                birthday = LocalDate.EPOCH
                type = REGISTERED
                groups = listOf(GroupModel().apply { id = groupId })
                project = ProjectModel().apply { id = projectId }
                purged = false
                create(currentUser())
            }

            // Act
            val result = repository.update(participant).block()

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(postgresRepository).findById(projectId, uuid, visibilitySearched = null, dateTimeSearched = null)
            verify(contentPostgresRepository).saveAll(any<Iterable<GroupContentEntity>>())
            verify(mapper).toEntity(any())
            verify(mapper, atLeastOnce()).toModel(any())
        }

        @Test
        @Order(3)
        fun `Should update call repository save and remove group from participant`() {
            // Arrange
            val participant = ParticipantModel().apply {
                id = uuid
                firstName = "test updated"
                lastName = "test updated"
                birthday = LocalDate.EPOCH
                type = REGISTERED
                project = ProjectModel().apply { id = projectId }
                purged = false
                create(currentUser())
            }

            // Act
            val result = repository.update(participant).block()

            // Assert
            assertNotNull(result)
            verify(postgresRepository).save(any())
            verify(postgresRepository).findById(projectId, uuid, visibilitySearched = null, dateTimeSearched = null)
            verify(contentPostgresRepository).deleteAllByParticipantIdAndGroupIds(uuid, listOf(groupId))
            verify(mapper).toEntity(any())
            verify(mapper, atLeastOnce()).toModel(any())
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
