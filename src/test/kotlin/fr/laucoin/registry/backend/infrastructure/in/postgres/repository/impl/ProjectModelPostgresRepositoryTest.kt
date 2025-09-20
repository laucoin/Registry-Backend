package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.ProjectEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IProjectEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.OffsetDateTime
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class ProjectModelPostgresRepositoryTest: TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IProjectEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: ProjectEntityMapper

	@Autowired
	private lateinit var port: IProjectPort

	companion object {
		@JvmStatic
		fun `Should validDateTime call repository validDateTime`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(ZonedDateTime.now(), ZonedDateTime.now(), false),
				Arguments.of(OffsetDateTime.MIN.toZonedDateTime(), OffsetDateTime.MIN.toZonedDateTime(), false),
				Arguments.of(OffsetDateTime.MAX.toZonedDateTime(), OffsetDateTime.MAX.toZonedDateTime(), false),
				Arguments.of(null, null, true),
				Arguments.of(OffsetDateTime.MIN.toZonedDateTime(), OffsetDateTime.MAX.toZonedDateTime(), true),
			)
		}
	}

	@Test
	fun `Should findPage call repository count and findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectSearchParamModel()

		// Act
		val result = port.findPage(pageable, params).block()

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
	fun `Should findPage call repository countAllInProjectIds and findAllInProjectIds`() {
		// Arrange
		val ids = listOf(projectId)
		val pageable = PageableModel(0, 10)
		val params = ProjectSearchParamModel()

		// Act
		val result = port.findPage(ids, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.pageNumber)
		assertEquals(10, result.pageSize)
		assertEquals(1, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findAllInProjectIds(
			ids,
			textSearched = null,
			visibilitySearched = null,
			dateTimeSearched = null,
			pageable.limit,
			pageable.offset,
		)
		verify(postgresRepository).countAllInProjectIds(
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
		begin: ZonedDateTime?,
		end: ZonedDateTime?,
		expected: Boolean,
	) {
		// Act
		val result = port.validDateTime(projectId, begin, end).block()

		// Assert
		assertEquals(expected, result)
		verify(postgresRepository).validDateTime(projectId, begin, end)
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result = port.findById(projectId, visibilitySearched = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			projectId,
			visibilitySearched = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findById call repository findById and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = port.findById(uuid, visibilitySearched = null).block()

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
			val project = ProjectModel().apply {
				name = "test"
				create(currentUser())
			}

			// Act
			val result = port.create(project).block()
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
			val project = ProjectModel().apply {
				name = "test update"
				create(currentUser())
			}

			// Act
			val result = port.update(project).block()

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
			port.deleteById(uuid).block()

			// Assert
			verify(postgresRepository).deleteById(uuid)
		}
	}
}
