package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IProjectEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.AfterEach
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IProjectEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: ProjectEntityMapper

	@Autowired
	private lateinit var port: IProjectPort

	private var createdProjectId: UUID? = null

	@AfterEach
	fun cleanupCreatedProject() {
		createdProjectId?.let { port.deleteById(it).block() }
		createdProjectId = null
	}

	private companion object {
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
	fun `Should findPage call repository findAll`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectSearchParamModel()

		// Act
		val result = port.findPage(pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(1, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findAll(
			query = null,
			isVisible = null,
			hiddenReadableIds = listOf(UUID(0, 0)),
			at = null,
			isFavorite = null,
			userId = null,
			sortField = null,
			isDescending = null,
			limit = pageable.limit,
			offset = pageable.offset,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findPage pass the favorite filter and the user to the repository`() {
		// Arrange
		val params = ProjectSearchParamModel(query = null, isFavorite = true).apply { userId = UUID.randomUUID() }

		// Act
		port.findPage(PageableModel(0, 10), params).block()

		// Assert
		verify(postgresRepository).findAll(
			query = null,
			isVisible = null,
			hiddenReadableIds = listOf(UUID(0, 0)),
			at = null,
			isFavorite = true,
			userId = params.userId,
			sortField = null,
			isDescending = null,
			limit = 10,
			offset = 0,
		)
	}

	@Test
	fun `Should findPage by ids pass the favorite filter and the user to the repository`() {
		// Arrange
		val ids = listOf(defaultProjectId)
		val params = ProjectSearchParamModel(query = null, isFavorite = true).apply { userId = UUID.randomUUID() }

		// Act
		port.findPage(ids, PageableModel(0, 10), params).block()

		// Assert
		verify(postgresRepository).findAllInProjectIds(
			ids,
			query = null,
			isVisible = null,
			hiddenReadableIds = listOf(UUID(0, 0)),
			at = null,
			isFavorite = true,
			userId = params.userId,
			sortField = null,
			isDescending = null,
			limit = 10,
			offset = 0,
		)
	}

	@Test
	fun `Should findPage call repository findAllInProjectIds`() {
		// Arrange
		val ids = listOf(defaultProjectId)
		val pageable = PageableModel(0, 10)
		val params = ProjectSearchParamModel()

		// Act
		val result = port.findPage(ids, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(1, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findAllInProjectIds(
			ids,
			query = null,
			isVisible = null,
			hiddenReadableIds = listOf(UUID(0, 0)),
			at = null,
			isFavorite = null,
			userId = null,
			sortField = null,
			isDescending = null,
			limit = pageable.limit,
			offset = pageable.offset,
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
		val result = port.validDateTime(defaultProjectId, begin, end).block()

		// Assert
		assertEquals(expected, result)
		verify(postgresRepository).validDateTime(defaultProjectId, begin, end)
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result = port.findById(defaultProjectId, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			defaultProjectId,
			isVisible = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findById fill the counts of the seeded project`() {
		// Arrange
		val id = defaultProjectId

		// Act
		val result = port.findById(id, isVisible = null).block()

		// Assert
		assertNotNull(result?.counts)
		assertTrue(result!!.counts!!.participants > 0)
		assertTrue(result.counts!!.acceptedProfiles > 0)
	}

	@Test
	fun `Should findPage fill the counts of every project`() {
		// Arrange
		val pageable = PageableModel(0, 10)

		// Act
		val result = port.findPage(pageable, ProjectSearchParamModel()).block()

		// Assert
		assertNotNull(result)
		assertTrue(result.content.isNotEmpty())
		assertTrue(result.content.all { it.counts != null })
	}

	@Test
	fun `Should create return zero counts and only the counts of enabled options`() {
		// Arrange
		val project = ProjectModel(name = "counts", options = listOf(ProjectOptionEnum.VEHICLE)).apply {
			create(currentUser())
		}

		// Act
		val result = port.create(project).block()
		createdProjectId = result?.id

		// Assert
		assertEquals(ProjectCountsModel(0, 0, 0, vehicles = 0), result?.counts)
	}

	@Test
	fun `Should findById call repository findById and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = port.findById(uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findById(
			uuid,
			isVisible = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findProjectsEligibleForPurge only select projects created before the threshold`() {
		// Act & Assert
		assertTrue(postgresRepository.findProjectsEligibleForPurge(LocalDate.EPOCH).collectList().block()!!.isEmpty())
		assertTrue(
			postgresRepository.findProjectsEligibleForPurge(LocalDate.now().plusYears(1)).collectList().block()!!
				.isNotEmpty()
		)
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
