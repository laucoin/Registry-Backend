package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import java.time.ZonedDateTime
import java.time.ZoneOffset
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IActivityEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.defaultActivityId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
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

class ActivityModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IActivityEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: ActivityEntityMapper

	@Autowired
	private lateinit var repository: IActivityPort

	private companion object {
		@JvmStatic
		fun `Should findPage accept every supported sort field`(): Stream<Arguments> =
			ActivitySearchParamModel.SORT_FIELDS.stream()
				.flatMap { Stream.of(Arguments.of(it, false), Arguments.of(it, true)) }

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
	fun `Should findPage call repository findAll`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ActivitySearchParamModel()

		// Act
		val result = repository.findPage(defaultProjectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(15, result.totalElements)
		assertEquals(2, result.totalPages)
		verify(postgresRepository).findAll(
			defaultProjectId,
			query = null,
			isVisible = null,
			isAvailable = null,
			at = null,
			minDuration = null,
			maxDuration = null,
			participants = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findPage accept every supported sort field`(field: String, isDescending: Boolean) {
		// Arrange
		val pageable = PageableModel(0, 200)
		val expectedCount =
			repository.findPage(defaultProjectId, pageable, ActivitySearchParamModel()).block()!!.content.size

		// Act
		val result = repository.findPage(
			defaultProjectId,
			pageable,
			ActivitySearchParamModel(query = null, sort = SortModel(field, isDescending)),
		).block()!!

		// Assert
		assertEquals(expectedCount, result.content.size)
	}

	@Test
	fun `Should findPage sort the activities by name in both directions`() {
		// Arrange
		val pageable = PageableModel(0, 200)

		// Act
		val ascending = repository.findPage(
			defaultProjectId, pageable, ActivitySearchParamModel(query = null, sort = SortModel("name")),
		).block()!!.content.map { it.name }
		val descending = repository.findPage(
			defaultProjectId, pageable, ActivitySearchParamModel(query = null, sort = SortModel("name", true)),
		).block()!!.content.map { it.name }

		// Assert
		assertEquals(ascending.reversed(), descending)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findAllByIds call repository findAllByIds`(
		ids: List<UUID>,
		expectedDatabaseCall: Int,
	) {
		// Act
		val result = repository.findAllByIds(defaultProjectId, ids, isVisible = null).collectList().block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository, times(expectedDatabaseCall)).findAllByIds(
			defaultProjectId,
			ids,
			isVisible = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findWithLimit call repository findWithLimit`() {
		// Arrange
		val size = 10
		val params = ActivitySearchParamModel()

		// Act
		val result = repository.findWithLimit(size, defaultProjectId, params).collectList().block()

		// Assert
		assertNotNull(result)
		assertEquals(size, result.size)
		verify(postgresRepository).findWithLimit(
			defaultProjectId,
			query = null,
			isVisible = null,
			isAvailable = null,
			at = null,
			minDuration = null,
			maxDuration = null,
			participants = null,
			size,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result = repository.findById(defaultProjectId, defaultActivityId, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			defaultProjectId,
			defaultActivityId,
			isVisible = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findById call repository findById and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = repository.findById(defaultProjectId, uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findById(
			defaultProjectId,
			uuid,
			isVisible = null,
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
			val activity = ActivityModel().apply {
				name = "test"
				description = "test"
				this.projectId = defaultProjectId
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
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
				this.projectId = defaultProjectId
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
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
