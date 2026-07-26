package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ActivitySortFieldEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IActivityEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
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
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActivityModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IActivityEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: ActivityEntityMapper

	@Autowired
	private lateinit var repository: IActivityPort

	private companion object {
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
		val result = repository.findPage(projectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.pageNumber)
		assertEquals(10, result.pageSize)
		assertEquals(15, result.totalElements)
		assertEquals(2, result.totalPages)
		verify(postgresRepository).findAll(
			projectId,
			textSearched = null,
			visibilitySearched = null,
			availabilitySearched = null,
			dateTimeSearched = null,
			pageable.limit,
			pageable.offset,
		)
		verify(postgresRepository).countAll(
			projectId,
			textSearched = null,
			visibilitySearched = null,
			availabilitySearched = null,
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
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findWithLimit call repository findWithLimit`() {
		// Arrange
		val size = 10
		val params = ActivitySearchParamModel()

		// Act
		val result = repository.findWithLimit(size, projectId, params).collectList().block()

		// Assert
		assertNotNull(result)
		assertEquals(size, result.size)
		verify(postgresRepository).findWithLimit(
			projectId,
			textSearched = null,
			visibilitySearched = null,
			availabilitySearched = null,
			dateTimeSearched = null,
			size,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result = repository.findById(projectId, activityId, visibilitySearched = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			projectId,
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
		val result = repository.findById(projectId, uuid, visibilitySearched = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findById(
			projectId,
			uuid,
			visibilitySearched = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findPage execute the sorted query and order by duration then name descending`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val params = ActivitySearchParamModel()
		val sort = listOf(
			SortModel(ActivitySortFieldEnum.DURATION),
			SortModel(ActivitySortFieldEnum.NAME, descending = true),
		)

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(15, result.totalElements)
		assertEquals(15, result.content.size)
		val expectedOrder = result.content
			.sortedWith(compareBy<ActivityModel> { it.duration!!.toIsoString() }.thenByDescending { it.name })
			.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}

	@Test
	fun `Should findPage combine the visibility filter with the sorted query`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val params = ActivitySearchParamModel(textSearched = null, visibilitySearched = true)
		val sort = listOf(SortModel(ActivitySortFieldEnum.NAME))

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(14, result.totalElements)
		assertEquals(14, result.content.size)
		assertTrue(result.content.all { it.visible })
		val expectedOrder = result.content.sortedBy { it.name }.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
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
				project = ProjectModel().apply { id = projectId }
				create(currentUser())
			}

			// Act
			val result = repository.create(activity).block()
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
			val activity = ActivityModel().apply {
				id = uuid
				name = "test update"
				description = "test update"
				project = ProjectModel().apply { id = projectId }
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
