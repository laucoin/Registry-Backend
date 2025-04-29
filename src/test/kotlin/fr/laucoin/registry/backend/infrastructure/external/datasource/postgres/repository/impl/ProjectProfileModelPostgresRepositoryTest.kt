package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ProjectProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ProjectProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ProjectProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IProjectProfileEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.projectProfileId
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

class ProjectProfileModelPostgresRepositoryTest(
    @Autowired private val repository: IProjectProfileModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IProjectProfileEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: ProjectProfileEntityMapper

    @MockitoSpyBean
    private lateinit var roleMapper: ProjectProfileRoleEntityMapper

    @MockitoSpyBean
    private lateinit var roleCountMapper: ProjectProfileRoleCountEntityMapper

    @Test
    fun `Should findProjectProfilesPageByUserId call repository countByUserId and findByUserId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = ProjectProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findProjectProfilesPageByUserId(currentUser().id !!, pageable, params).block()

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
    fun `Should findProjectProfilesPageByProjectId call repository countByProjectId and findByProjectId`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = ProjectProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findProjectProfilesPageByProjectId(projectId, pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(2, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findByProjectId(
            projectId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            dateTimeSearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countByProjectId(
            projectId,
            textSearched = null,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            dateTimeSearched = null,
        )
        verify(mapper, times(2)).toModel(any())
    }

    @Test
    fun `Should findUserIdsWithProjectProfileForProjectWithProfileExclusion call repository findUserIdsWithProjectProfileForProjectWithProfileExclusion`() {
        // Act
        val result = repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
            projectId,
            listOf(currentUser().id !!),
            profileIdToExclude = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            startDateTimeSearched = null,
            endDateTimeSearched = null
        ).collectList().block()

        // Assert
        assertEquals(1, result?.size)
        verify(postgresRepository).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
            projectId,
            listOf(currentUser().id !!),
            profileIdToExclude = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
            startDateTimeSearched = null,
            endDateTimeSearched = null
        )
    }

    @Test
    fun `Should findProjectProfilesRolesByUserId call repository findAllRolesByUserId`() {
        // Act
        val result = repository.findProjectProfilesRolesByUserId(currentUser().id !!)
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
    fun `Should findProjectProfileByUserIdAndId call repository findByUserIdAndId`() {
        // Act
        val result = repository.findProjectProfileByUserIdAndId(
            currentUser().id !!,
            projectProfileId,
            visibilitySearched = null
        ).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findByUserIdAndId(
            currentUser().id !!,
            projectProfileId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findProjectProfileByUserIdAndId call repository findByUserIdAndId and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findProjectProfileByUserIdAndId(currentUser().id !!, uuid, visibilitySearched = null).block()

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
    fun `Should findById call repository findByProjectIdAndId`() {
        // Act
        val result = repository.findById(projectId, projectProfileId, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findByProjectIdAndId(
            projectId,
            projectProfileId,
            visibilitySearched = null,
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findById call repository findByProjectIdAndId and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findById(projectId, uuid, visibilitySearched = null).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findByProjectIdAndId(
            projectId,
            uuid,
            visibilitySearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findProjectProfileByProjectAndUserId call repository findUsableProfileByProjectAndUserId`() {
        // Arrange
        val params = ProjectProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findProjectProfileByProjectAndUserId(projectId, currentUser().id !!, params).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findProjectProfileByProjectAndUserId(
            projectId,
            currentUser().id !!,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
        )
        verify(mapper).toModel(any())
    }

    @Test
    fun `Should findProjectProfileByProjectAndUserId call repository findUsableProfileByProjectAndUserId and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()
        val params = ProjectProfileSearchParamModel(statusSearched = null)

        // Act
        val result = repository.findProjectProfileByProjectAndUserId(projectId, uuid, searchParams = params).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findProjectProfileByProjectAndUserId(
            projectId,
            uuid,
            visibilitySearched = null,
            availabilitySearched = null,
            statusSearched = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findLevel0ProjectProfileRoleByUserId call repository findLevel0ProjectProfileRoleByUserId`() {
        // Act
        val result =
            repository.findLevel0ProjectProfileRoleByUserId(currentUser().id !!, visibilitySearched = null).collectList().block()

        // Assert
        assertFalse(result.isNullOrEmpty())
        verify(postgresRepository).findLevel0ProjectProfileRoleByUserId(
            currentUser().id !!,
            visibilitySearched = null,
        )
        verify(roleCountMapper).toModel(any())
    }

    @Test
    fun `Should findLevel0ProjectProfileRoleByProjectId call repository findLevel0ProjectProfileRoleByProjectId`() {
        // Act
        val result =
            repository.findLevel0ProjectProfileRoleByProjectId(projectId, visibilitySearched = null).collectList().block()

        // Assert
        assertFalse(result.isNullOrEmpty())
        verify(postgresRepository).findLevel0ProjectProfileRoleByProjectId(
            projectId,
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
            val projectProfile = ProjectProfileModel().apply {
                user = UserModel().apply { id = userIdWithoutProfile }
                project = ProjectModel().apply { id = projectId }
                role = "PROJECT_ADMINISTRATOR"
                status = INVITED
                create(currentUser())
            }

            // Act
            val result = repository.create(projectProfile).block()
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
            val projectProfile = ProjectProfileModel().apply {
                user = UserModel().apply { id = userIdWithoutProfile }
                project = ProjectModel().apply { id = projectId }
                role = "PROJECT_ADMINISTRATOR"
                status = ACCEPTED
                create(currentUser())
            }

            // Act
            val result = repository.update(projectProfile).block()

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
            val projectProfile = ProjectProfileModel().apply {
                user = UserModel().apply { id = userIdWithoutProfile }
                project = ProjectModel().apply { id = projectId }
                role = "PROJECT_ADMINISTRATOR"
                status = INVITED
                create(currentUser())
            }

            // Act
            val result = repository.saveAll(listOf(projectProfile)).collectList().block()

            // Assert
            assertFalse(result.isNullOrEmpty())
            verify(mapper).toEntity(any())
            verify(mapper).toModel(any())
        }
    }
}
