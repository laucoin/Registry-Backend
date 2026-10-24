package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IUserEntityRepository
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.Assertions.assertFalse
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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

class UserModelPostgresRepositoryTest: TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IUserEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: UserEntityMapper

	@MockitoSpyBean
	private lateinit var currentUserMapper: CurrentUserEntityMapper

	@Autowired
	private lateinit var repository: IUserPort

	@Test
	fun `Should findPage call repository findAll`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = UserSearchParamModel()

		// Act
		val result = repository.findPage(pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(5, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findAll(
			query = null,
			isVisible = null,
			role = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findPage keep only the users with the requested role`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val all = repository.findPage(pageable, UserSearchParamModel()).block()!!.content
		val role = all.mapNotNull { it.role }.first()

		// Act
		val result = repository.findPage(pageable, UserSearchParamModel(query = null, role = role)).block()!!

		// Assert
		assertTrue(result.content.isNotEmpty())
		assertTrue(result.content.size < all.size)
		assertEquals(all.count { it.role == role }, result.content.size)
		assertTrue(result.content.all { it.role == role })
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
			query = null,
			isVisible = null,
			size,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result = repository.findById(currentUser().id!!, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			currentUser().id!!,
			isVisible = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findById call repository findById and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = repository.findById(uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findById(
			uuid,
			isVisible = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findByOidcId call repository findByOidcId`() {
		// Act
		val result = repository.findByOidcId(currentUser().oidcId!!, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findByOidcId(
			currentUser().oidcId!!,
			isVisible = null,
		)
		verify(currentUserMapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findByOidcId call repository findByOidcId and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = repository.findByOidcId(uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findByOidcId(
			uuid,
			isVisible = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findByRoleLevel call repository findByRoleLevel and admin`() {
		// Arrange
		val level = 0

		// Act
		val result = repository.findByRoleLevel(level, isVisible = null).collectList().block()

		// Assert
		assertEquals(3, result?.size)
		assertEquals(currentUser().id, result?.first()?.id)
		verify(postgresRepository).findByRoleLevel(
			level,
			isVisible = null,
		)
	}

	@Test
	fun `Should lockLevel0Users call repository lockLevel0Users`() {
		// Act
		val result = repository.lockLevel0Users().collectList().block()

		// Assert
		assertFalse(result.isNullOrEmpty())
		verify(postgresRepository).lockLevel0Users()
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
			uuid = result!!.id!!

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
