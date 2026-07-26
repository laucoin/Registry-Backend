package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IMovementEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertNotNull
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
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovementModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IMovementEntityRepository

	@MockitoSpyBean
	private lateinit var contentPostgresRepository: IMovementContentEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: MovementEntityMapper

	@MockitoSpyBean
	private lateinit var contentMapper: MovementContentEntityMapper

	@Autowired
	private lateinit var repository: IMovementPort

	private companion object {
		@JvmStatic
		fun `Should findContent call contentRepository findAllByMovementIds`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					listOf(movementId),
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
		val params = MovementSearchParamModel(typeSearched = null)

		// Act
		val result = repository.findPage(projectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.pageNumber)
		assertEquals(10, result.pageSize)
		assertEquals(60, result.totalElements)
		assertEquals(6, result.totalPages)
		verify(postgresRepository).findAll(
			projectId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
			pageable.limit,
			pageable.offset,
		)
		verify(postgresRepository).countAll(
			projectId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findPageByParticipantId call repository countAllByParticipantId and findAllByParticipantId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = null)

		// Act
		val result = repository.findPageByParticipantId(projectId, participantId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.pageNumber)
		assertEquals(10, result.pageSize)
		verify(postgresRepository).findAllByParticipantId(
			projectId,
			participantId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
			pageable.limit,
			pageable.offset,
		)
		verify(postgresRepository).countAllByParticipantId(
			projectId,
			participantId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findPageByVehicleId call repository countAllByVehicleId and findAllByVehicleId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = null)

		// Act
		val result = repository.findPageByVehicleId(projectId, vehicleId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.pageNumber)
		assertEquals(10, result.pageSize)
		verify(postgresRepository).findAllByVehicleId(
			projectId,
			vehicleId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
			pageable.limit,
			pageable.offset,
		)
		verify(postgresRepository).countAllByVehicleId(
			projectId,
			vehicleId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
	}

	@Test
	fun `Should findPageByActivityId call repository countAllByActivityId and findAllByActivityId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(typeSearched = null)

		// Act
		val result = repository.findPageByActivityId(projectId, activityId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.pageNumber)
		assertEquals(10, result.pageSize)
		verify(postgresRepository).findAllByActivityId(
			projectId,
			activityId,
			visibilitySearched = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
			pageable.limit,
			pageable.offset,
		)
		verify(postgresRepository).countAllByActivityId(
			projectId,
			activityId,
			visibilitySearched = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findContent call contentRepository findAllByMovementIds`(
		ids: List<UUID>,
		expectedContentRepositoryCall: Int,
	) {
		// Act
		repository.findContent(projectId, ids).collectList().block()

		// Assert
		verify(contentPostgresRepository, times(expectedContentRepositoryCall)).findAllByMovementIds(
			projectId,
			ids,
		)
		verify(contentMapper, atLeast(expectedContentRepositoryCall)).toModel(any())
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result = repository.findById(projectId, movementId, visibilitySearched = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			projectId,
			movementId,
			visibilitySearched = null,
		)
		verify(contentPostgresRepository).findAllByMovementIds(
			projectId,
			listOf(movementId),
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findOngoingActivities execute its CTE query against the database`() {
		// Act
		val result = repository.findOngoingActivities(projectId, limit = 200).collectList().block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findOngoingActivities(projectId, visibilitySearched = true, limit = 200)
		assertTrue(result!!.all { it.type == OUT && Objects.nonNull(it.activity) })
	}

	@Test
	fun `Should countAllByParticipantId call repository countAllByParticipantId`() {
		// Act
		val result = repository.countAllByParticipantId(projectId, participantId, MovementSearchParamModel()).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).countAllByParticipantId(
			projectId,
			participantId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should countAllByVehicleId call repository countAllByVehicleId`() {
		// Act
		val result = repository.countAllByVehicleId(projectId, vehicleId, MovementSearchParamModel()).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).countAllByVehicleId(
			projectId,
			vehicleId,
			visibilitySearched = null,
			linkedToActivity = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should countAllByActivityId call repository countAllByActivityId`() {
		// Act
		val result = repository.countAllByActivityId(projectId, activityId, MovementSearchParamModel()).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).countAllByActivityId(
			projectId,
			activityId,
			visibilitySearched = null,
			typeSearched = listOf(IN, OUT),
			startDateTimeSearched = null,
			endDateTimeSearched = null,
		)
		verify(mapper, never()).toModel(any())
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
		verify(contentPostgresRepository).findAllByMovementIds(
			projectId,
			listOf(uuid),
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findPage execute the sorted query and order by type then dateTime descending`() {
		// Arrange
		val pageable = PageableModel(0, 60)
		val params = MovementSearchParamModel(typeSearched = null)
		val sort = listOf(
			SortModel(MovementSortFieldEnum.TYPE),
			SortModel(MovementSortFieldEnum.DATE_TIME, descending = true),
		)

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(60, result!!.totalElements)
		assertEquals(60, result.content.size)
		val expectedOrder = result.content
			.sortedWith(compareBy<MovementModel> { it.type!!.name }.thenByDescending { it.dateTime.toInstant() })
			.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}

	@Test
	fun `Should findPage combine the visibility filter with the sorted query`() {
		// Arrange
		val pageable = PageableModel(0, 60)
		val params = MovementSearchParamModel(visibilitySearched = true, typeSearched = null)
		val sort = listOf(
			SortModel(MovementSortFieldEnum.DATE_TIME),
			SortModel(MovementSortFieldEnum.TYPE, descending = true),
		)

		// Act
		val result = repository.findPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertEquals(59, result!!.totalElements)
		assertEquals(59, result.content.size)
		assertTrue(result.content.all { it.visible })
		val expectedOrder = result.content
			.sortedWith(compareBy<MovementModel> { it.dateTime.toInstant() }.thenByDescending { it.type!!.name })
			.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}

	@Test
	fun `Should findCurrentPage execute the sorted current query with a consistent total`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val params = MovementSearchParamModel(typeSearched = null)
		val sort = listOf(
			SortModel(MovementSortFieldEnum.TYPE),
			SortModel(MovementSortFieldEnum.DATE_TIME, descending = true),
		)

		// Act
		val result = repository.findCurrentPage(projectId, pageable, params, sort).block()

		// Assert
		assertNotNull(result)
		assertTrue(result!!.content.isNotEmpty())
		assertEquals(result.content.size.toLong(), result.totalElements)
		val expectedOrder = result.content
			.sortedWith(compareBy<MovementModel> { it.type!!.name }.thenByDescending { it.dateTime.toInstant() })
			.map { it.id }
		assertEquals(expectedOrder, result.content.map { it.id })
	}

	/**
	 * The ongoing-activities panel is read per PARTICIPANT, not per activity: an outing
	 * stops being ongoing the moment someone checks its people back in, and a plain entry
	 * that does not name the activity again ends it just as much as one that does
	 * (functional/features/dashboards.md — "Ongoing activities"). Keying on the activity's
	 * own last movement instead left a returned group showing as still out, which the panel
	 * exists to prevent.
	 */
	@Nested
	@TestInstance(PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
	inner class OngoingActivityTests {
		private val outing: MovementModel = MovementModel(contentType = REGISTERED).apply {
			dateTime = ZonedDateTime.now().minusHours(2)
			type = OUT
			activity = ActivityModel().apply { id = activityId }
			project = ProjectModel().apply { id = projectId }
			content = listOf(MovementContentModel().apply {
				participant = ParticipantModel().apply { id = participantId }
			})
		}
		private lateinit var outingId: UUID
		private lateinit var returnId: UUID

		@Test
		@Order(1)
		fun `Should list an outing whose participant is still out`() {
			// Arrange
			outingId = repository.create(outing.apply { create(currentUser()) }).block()!!.id!!

			// Act
			val result = repository.findOngoingActivities(projectId, limit = 200).collectList().block()

			// Assert
			assertTrue(result!!.any { it.id == outingId })
		}

		@Test
		@Order(2)
		fun `Should drop the outing once a plain entry checks its participant back in`() {
			// Arrange
			val back = MovementModel(contentType = REGISTERED).apply {
				dateTime = ZonedDateTime.now()
				type = IN
				project = ProjectModel().apply { id = projectId }
				content = listOf(MovementContentModel().apply {
					participant = ParticipantModel().apply { id = participantId }
				})
				create(currentUser())
			}
			returnId = repository.create(back).block()!!.id!!

			// Act
			val result = repository.findOngoingActivities(projectId, limit = 200).collectList().block()

			// Assert
			assertTrue(result!!.none { it.id == outingId })
		}

		@Test
		@Order(3)
		fun `Should clean up the movements it created`() {
			// Act
			repository.deleteById(returnId).block()
			repository.deleteById(outingId).block()

			// Assert
			assertNull(repository.findById(projectId, outingId, visibilitySearched = null).block())
			assertNull(repository.findById(projectId, returnId, visibilitySearched = null).block())
		}
	}

	@Nested
	@TestInstance(PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
	inner class WritingTests {
		private val movementDateTime: ZonedDateTime = ZonedDateTime.now()
		private lateinit var uuid: UUID

		@Test
		@Order(1)
		fun `Should create call repository save`() {
			// Arrange
			val movement = MovementModel(contentType = REGISTERED).apply {
				dateTime = movementDateTime
				type = IN
				project = ProjectModel().apply { id = projectId }
				create(currentUser())
			}

			// Act
			val result = repository.create(movement).block()
			uuid = result!!.id!!

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(mapper).toEntity(any())
			verify(mapper).toModel(any())
		}

		@Test
		@Order(2)
		fun `Should update call repository save and add member in movement`() {
			// Arrange
			val movement = MovementModel(contentType = REGISTERED).apply {
				id = uuid
				dateTime = movementDateTime
				type = IN
				project = ProjectModel().apply { id = projectId }
				content = listOf(MovementContentModel().apply {
					participant = ParticipantModel().apply { id = participantId }
				})
				create(currentUser())
			}

			// Act
			val result = repository.update(movement).block()

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(postgresRepository).findById(projectId, uuid, visibilitySearched = null)
			verify(contentPostgresRepository).findAllByMovementIds(projectId, listOf(uuid))
			verify(mapper).toEntity(any())
			verify(mapper, times(2)).toModel(any())
		}

		@Test
		@Order(3)
		fun `Should update call repository save and remove member in movement`() {
			// Arrange
			val movement = MovementModel(contentType = REGISTERED).apply {
				id = uuid
				dateTime = movementDateTime
				type = IN
				project = ProjectModel().apply { id = projectId }
				content = emptyList()
				create(currentUser())
			}

			// Act
			val result = repository.update(movement).block()

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(postgresRepository).findById(projectId, uuid, visibilitySearched = null)
			verify(contentPostgresRepository).findAllByMovementIds(projectId, listOf(uuid))
			verify(mapper).toEntity(any())
			verify(mapper, times(2)).toModel(any())
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
