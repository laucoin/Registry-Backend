package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.GroupSortFieldEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IGroupEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.groupId
import fr.laucoin.registry.backend.test.ModelExt.participantId
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
		fun `Should execute the due-today group queries against the database`(): Stream<Arguments> =
			Stream.of(Arguments.of(true), Arguments.of(false))

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
					listOf(groupId),
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
		val params = GroupSearchParamModel()

		// Act
		val result = repository.findPage(projectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.pageNumber)
		assertEquals(10, result.pageSize)
		assertEquals(20, result.totalElements)
		assertEquals(2, result.totalPages)
		verify(postgresRepository).findAll(
			projectId,
			textSearched = null,
			visibilitySearched = null,
			presenceSearched = null,
			dateTimeSearched = null,
			pageable.limit,
			pageable.offset,
		)
		verify(postgresRepository).countAll(
			projectId,
			textSearched = null,
			visibilitySearched = null,
			presenceSearched = null,
			dateTimeSearched = null,
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
		val visibilitySearched = null
		val availabilitySearched = null
		repository.findContent(projectId, ids, visibilitySearched, availabilitySearched).collectList().block()

		// Assert
		verify(contentPostgresRepository, times(expectedContentRepositoryCall)).findAllByGroupIds(
			projectId,
			ids,
			visibilitySearched,
			availabilitySearched,
		)
		verify(contentMapper, atLeast(expectedContentRepositoryCall)).toModel(any())
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
		val params = GroupSearchParamModel()

		// Act
		val result = repository.findWithLimit(size, projectId, params).collectList().block()

		// Assert
		assertNotNull(result)
		assertEquals(size, result.size)
		verify(postgresRepository).findWithLimit(
			projectId,
			textSearched = null,
			visibilitySearched = null,
			presenceSearched = null,
			dateTimeSearched = null,
			size,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result =
			repository.findByIdWithContent(
				projectId,
				groupId,
				visibilitySearched = null,
				memberVisibilitySearched = null,
				memberAvailabilitySearched = null
			).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			projectId,
			groupId,
			visibilitySearched = null,
		)
		verify(contentPostgresRepository).findAllByGroupIds(
			projectId,
			listOf(groupId),
			visibilitySearched = null,
			availabilitySearched = null,
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
				projectId,
				uuid,
				visibilitySearched = null,
				memberVisibilitySearched = null,
				memberAvailabilitySearched = null
			).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findById(
			projectId,
			uuid,
			visibilitySearched = null,
		)
		verify(contentPostgresRepository).findAllByGroupIds(
			projectId,
			listOf(uuid),
			visibilitySearched = null,
			availabilitySearched = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findPage execute the sorted query and order by name then startAvailabilityDate descending`() {
		// Arrange
		val pageable = PageableModel(0, 25)
		val params = GroupSearchParamModel()
		val sort = listOf(
			SortModel(GroupSortFieldEnum.NAME),
			SortModel(GroupSortFieldEnum.START_AVAILABILITY_DATE, descending = true),
		)

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(20, result.totalElements)
		assertEquals(20, result.content.size)
		val expectedOrder = result.content.sortedBy { it.name }.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}

	@Test
	fun `Should findPage combine the visibility filter with the sorted query`() {
		// Arrange
		val pageable = PageableModel(0, 25)
		val params = GroupSearchParamModel(textSearched = null, visibilitySearched = true)
		val sort = listOf(SortModel(GroupSortFieldEnum.NAME, descending = true))

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(18, result.totalElements)
		assertEquals(18, result.content.size)
		assertTrue(result.content.all { it.visible })
		val expectedOrder = result.content.sortedByDescending { it.name }.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}

	/**
	 * The "due today" group queries carry correlated EXISTS sub-selects over
	 * group content, participants and movements — exactly the shape a
	 * hand-written @Query gets wrong silently. Run against the real database so a
	 * broken join or a bad column name fails here rather than emptying a
	 * dashboard panel in production.
	 */
	@ParameterizedTest
	@MethodSource
	fun `Should execute the due-today group queries against the database`(arriving: Boolean) {
		// Act
		val result = (if (arriving) repository.findArrivingToday(projectId, visibilitySearched = true, limit = 200)
		else repository.findDepartingToday(projectId, visibilitySearched = true, limit = 200))
			.collectList().block()

		// Assert
		assertNotNull(result)
		assertTrue(result.all { it.visible })
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
				project = ProjectModel().apply { id = projectId }
				create(currentUser())
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
				project = ProjectModel().apply { id = projectId }
				members = listOf(ParticipantModel().apply { id = participantId })
				create(currentUser())
			}

			// Act
			repository.update(group).block()

			// Assert
			verify(postgresRepository).save(any())
			verify(postgresRepository).findById(projectId, uuid, visibilitySearched = null)
			verify(contentPostgresRepository).findAllByGroupIds(
				projectId,
				listOf(uuid),
				visibilitySearched = null,
				availabilitySearched = null
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
				project = ProjectModel().apply { id = projectId }
				members = emptyList()
				create(currentUser())
			}

			// Act
			repository.update(group).block()

			// Assert
			verify(postgresRepository).save(any())
			verify(postgresRepository).findById(projectId, uuid, visibilitySearched = null)
			verify(contentPostgresRepository).findAllByGroupIds(
				projectId,
				listOf(uuid),
				visibilitySearched = null,
				availabilitySearched = null
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
