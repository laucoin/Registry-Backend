package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IUserEntityRepository
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
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
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class UserModelPostgresRepositoryTest(
    @Autowired private val repository: IUserModelRepository
): TestContext() {
    @MockitoSpyBean
    private lateinit var postgresRepository: IUserEntityRepository

    @MockitoSpyBean
    private lateinit var mapper: UserEntityMapper

    @MockitoSpyBean
    private lateinit var currentUserMapper: CurrentUserEntityMapper

    @Test
    fun `Should findPage call repository count and findPage`() {
        // Arrange
        val pageable = PageableModel(0, 10)
        val params = UserSearchParamModel()

        // Act
        val result = repository.findPage(pageable, params).block()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.pageNumber)
        assertEquals(10, result.pageSize)
        assertEquals(5, result.totalElements)
        assertEquals(1, result.totalPages)
        verify(postgresRepository).findAll(
            textSearched = null,
            visibilitySearched = null,
            pageable.limit,
            pageable.offset,
        )
        verify(postgresRepository).countAll(
            textSearched = null,
            visibilitySearched = null,
        )
        verify(mapper, atLeastOnce()).toModel(any())
    }

    @Test
    fun `Should findWithLimit call repository findWithLimit`() {
        // Arrange
        val size = 10
        val params = UserSearchParamModel()

        // Act
        val result = repository.findWithLimit(size, params).collectList().block()

        // Assert
        assertNotNull(result)
        assertEquals(5, result.size)
        verify(postgresRepository).findWithLimit(
            textSearched = null,
            visibilitySearched = null,
            size,
        )
        verify(mapper, atLeastOnce()).toModel(any())
    }

    @Test
    fun `Should findById call repository findById`() {
        // Act
        val result = repository.findById(currentUser().id !!, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findById(
            currentUser().id !!,
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

    @Test
    fun `Should findByOidcId call repository findByOidcId`() {
        // Act
        val result = repository.findByOidcId(currentUser().oidcId !!, visibilitySearched = null).block()

        // Assert
        assertNotNull(result)
        verify(postgresRepository).findByOidcId(
            currentUser().oidcId !!,
            visibilitySearched = null,
        )
        verify(currentUserMapper, atLeastOnce()).toModel(any())
    }

    @Test
    fun `Should findByOidcId call repository findByOidcId and return null`() {
        // Arrange
        val uuid = UUID.randomUUID()

        // Act
        val result = repository.findByOidcId(uuid, visibilitySearched = null).block()

        // Assert
        assertNull(result)
        verify(postgresRepository).findByOidcId(
            uuid,
            visibilitySearched = null,
        )
        verify(mapper, never()).toModel(any())
    }

    @Test
    fun `Should findByRoleLevel call repository findByRoleLevel and admin`() {
        // Arrange
        val level = 0

        // Act
        val result = repository.findByRoleLevel(level, visibilitySearched = null).collectList().block()

        // Assert
        assertEquals(3, result?.size)
        assertEquals(currentUser().id, result?.first()?.id)
        verify(postgresRepository).findByRoleLevel(
            level,
            visibilitySearched = null,
        )
    }

    @Test
    fun `Should findServiceAccount call repository findServiceAccount`() {
        // Act
        val result = repository.findServiceAccount().block()

        // Assert
        assertNotNull(result)
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
            val user = UserModel().apply {
                firstName = "test"
                lastName = "test"
                create(currentUser())
            }

            // Act
            val result = repository.create(user).block()
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
            val user = UserModel().apply {
                id = uuid
                firstName = "test update"
                lastName = "test update"
                create(currentUser())
            }

            // Act
            val result = repository.update(user).block()

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
