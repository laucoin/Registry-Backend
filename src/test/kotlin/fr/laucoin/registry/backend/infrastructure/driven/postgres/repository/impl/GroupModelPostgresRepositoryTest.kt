package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import java.time.ZonedDateTime
import java.time.ZoneOffset
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.defaultGroupId
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipantId
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
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class GroupModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IGroupEntityRepository

	@MockitoSpyBean
	private lateinit var contentPostgresRepository: IGroupContentEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: GroupEntityMapper

	@MockitoSpyBean
	private lateinit var contentMapper: GroupContentEntityMapper

	@Autowired
	private lateinit var repository: IGroupPort

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

		@JvmStatic
		fun `Should findContent call contentRepository findAllByGroupIds`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					listOf(defaultGroupId),
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
		val params = GroupSearchParamModel()

		// Act
		val result = repository.findPage(defaultProjectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(20, result.totalElements)
		assertEquals(2, result.totalPages)
		verify(postgresRepository).findAll(
			defaultProjectId,
			query = null,
			isVisible = null,
			isPresent = null,
			at = null,
			memberUserId = null,
			hasOnSiteMembers = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findContent call contentRepository findAllByGroupIds`(
		ids: List<UUID>,
		expectedContentRepositoryCall: Int,
	) {
		// Act
		val isVisible = null
		val isAvailable = null
		repository.findContent(defaultProjectId, ids, isVisible, isAvailable).collectList().block()

		// Assert
		verify(contentPostgresRepository, times(expectedContentRepositoryCall)).findAllByGroupIds(
			defaultProjectId,
			ids,
			isVisible,
			isAvailable,
		)
		verify(contentMapper, atLeast(expectedContentRepositoryCall)).toModel(any())
	}

	@ParameterizedTest
	@MethodSource("Should findAllByIds call repository findAllByIds")
	fun `Should lockByIds call repository lockByIds only when ids are given`(
		ids: List<UUID>,
		expectedDatabaseCall: Int,
	) {
		// Act
		val result = repository.lockByIds(ids).collectList().block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository, times(expectedDatabaseCall)).lockByIds(ids)
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
	fun `Should findSummaryById return the group with its last edition and without its members`() {
		// Act
		val result = repository.findSummaryById(defaultProjectId, defaultGroupId).block()

		// Assert
		assertNotNull(result)
		assertEquals(defaultGroupId, result.id)
		assertNotNull(result.lastEdition)
		assertTrue(result.members.isEmpty())
		verify(postgresRepository).findSummaryById(defaultProjectId, defaultGroupId)
	}

	@Test
	fun `Should findSummaryById return nothing for a group of another project`() {
		// Act
		val result = repository.findSummaryById(UUID.randomUUID(), defaultGroupId).block()

		// Assert
		assertNull(result)
	}

	@Test
	fun `Should findWithLimit call repository findWithLimit`() {
		// Arrange
		val size = 10
		val params = GroupSearchParamModel()

		// Act
		val result = repository.findWithLimit(size, defaultProjectId, params).collectList().block()

		// Assert
		assertNotNull(result)
		assertEquals(size, result.size)
		verify(postgresRepository).findWithLimit(
			defaultProjectId,
			query = null,
			isVisible = null,
			isPresent = null,
			at = null,
			size,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result =
			repository.findByIdWithContent(
				defaultProjectId,
				defaultGroupId,
				isVisible = null,
				memberIsVisible = null,
				memberIsAvailable = null
			).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			defaultProjectId,
			defaultGroupId,
			isVisible = null,
		)
		verify(contentPostgresRepository).findAllByGroupIds(
			defaultProjectId,
			listOf(defaultGroupId),
			isVisible = null,
			isAvailable = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findById call repository findById and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result =
			repository.findByIdWithContent(
				defaultProjectId,
				uuid,
				isVisible = null,
				memberIsVisible = null,
				memberIsAvailable = null
			).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findById(
			defaultProjectId,
			uuid,
			isVisible = null,
		)
		verify(contentPostgresRepository).findAllByGroupIds(
			defaultProjectId,
			listOf(uuid),
			isVisible = null,
			isAvailable = null,
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
			val group = GroupModel().apply {
				name = "test"
				this.projectId = defaultProjectId
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
			}

			// Act
			val result = repository.create(group).block()
			uuid = result!!.id!!

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(mapper).toEntity(any())
			verify(mapper).toModel(any())
		}

		@Test
		@Order(2)
		fun `Should update call repository save and add member in group`() {
			// Arrange
			val group = GroupModel().apply {
				id = uuid
				name = "test update"
				this.projectId = defaultProjectId
				members = listOf(ParticipantModel().apply { id = defaultParticipantId })
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
			}

			// Act
			repository.update(group).block()

			// Assert
			verify(postgresRepository).save(any())
			verify(postgresRepository).findById(defaultProjectId, uuid, isVisible = null)
			verify(contentPostgresRepository).findAllByGroupIds(
				defaultProjectId,
				listOf(uuid),
				isVisible = null,
				isAvailable = null
			)
			verify(mapper).toEntity(any())
			verify(mapper, atLeastOnce()).toModel(any())
		}

		@Test
		@Order(3)
		fun `Should update call repository save and remove member in group`() {
			// Arrange
			val group = GroupModel().apply {
				id = uuid
				name = "test update"
				this.projectId = defaultProjectId
				members = emptyList()
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
			}

			// Act
			repository.update(group).block()

			// Assert
			verify(postgresRepository).save(any())
			verify(postgresRepository).findById(defaultProjectId, uuid, isVisible = null)
			verify(contentPostgresRepository).findAllByGroupIds(
				defaultProjectId,
				listOf(uuid),
				isVisible = null,
				isAvailable = null
			)
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
