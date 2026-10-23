package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IMovementEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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

class MovementModelPostgresRepositoryTest: TestContext() {
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
	fun `Should findPage call repository findAll`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = null)

		// Act
		val result = repository.findPage(projectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.page)
		assertEquals(10, result.size)
		assertEquals(60, result.totalElements)
		assertEquals(6, result.totalPages)
		verify(postgresRepository).findAll(
			projectId,
			isVisible = null,
			hasActivity = null,
			activityId = null,
			type = listOf(IN, OUT),
			from = null,
			to = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findPage keep only the movements of the requested activity`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val all = repository.findPage(projectId, pageable, MovementSearchParamModel(type = null)).block()!!.content
		val activityId = all.firstNotNullOf { it.activity?.id }

		// Act
		val result = repository.findPage(
			projectId, pageable, MovementSearchParamModel(type = null, activityId = activityId),
		).block()!!

		// Assert
		assertEquals(all.count { it.activity?.id == activityId }, result.content.size)
		assertTrue(result.content.all { it.activity?.id == activityId })
	}

	@Test
	fun `Should findCurrentPage keep only the current movements of the requested activity`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val current = repository.findCurrentPage(projectId, pageable, MovementSearchParamModel(type = null)).block()!!.content
		val activityId = repository.findPage(projectId, pageable, MovementSearchParamModel(type = null)).block()!!
			.content.firstNotNullOf { it.activity?.id }

		// Act
		val result = repository.findCurrentPage(
			projectId, pageable, MovementSearchParamModel(type = null, activityId = activityId),
		).block()!!

		// Assert
		assertEquals(current.count { it.activity?.id == activityId }, result.content.size)
		assertEquals(result.content.size.toLong(), result.totalElements)
		assertTrue(result.content.all { it.activity?.id == activityId })
	}

	@Test
	fun `Should findPageByParticipantId call repository findAllByParticipantId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = null)

		// Act
		val result = repository.findPageByParticipantId(projectId, participantId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.page)
		assertEquals(10, result.size)
		verify(postgresRepository).findAllByParticipantId(
			projectId,
			participantId,
			isVisible = null,
			hasActivity = null,
			type = listOf(IN, OUT),
			from = null,
			to = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findPageByVehicleId call repository findAllByVehicleId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = null)

		// Act
		val result = repository.findPageByVehicleId(projectId, vehicleId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.page)
		assertEquals(10, result.size)
		verify(postgresRepository).findAllByVehicleId(
			projectId,
			vehicleId,
			isVisible = null,
			hasActivity = null,
			type = listOf(IN, OUT),
			from = null,
			to = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
	}

	@Test
	fun `Should findPageByActivityId call repository findAllByActivityId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = MovementSearchParamModel(type = null)

		// Act
		val result = repository.findPageByActivityId(projectId, activityId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result!!.page)
		assertEquals(10, result.size)
		verify(postgresRepository).findAllByActivityId(
			projectId,
			activityId,
			isVisible = null,
			type = listOf(IN, OUT),
			from = null,
			to = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
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
		val result = repository.findById(projectId, movementId, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			projectId,
			movementId,
			isVisible = null,
		)
		verify(contentPostgresRepository).findAllByMovementIds(
			projectId,
			listOf(movementId),
		)
		verify(mapper).toModel(any())
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
			isVisible = null,
			hasActivity = null,
			type = listOf(IN, OUT),
			from = null,
			to = null,
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
			isVisible = null,
			hasActivity = null,
			type = listOf(IN, OUT),
			from = null,
			to = null,
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
			isVisible = null,
			type = listOf(IN, OUT),
			from = null,
			to = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findById call repository findById and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = repository.findById(projectId, uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findById(
			projectId,
			uuid,
			isVisible = null,
		)
		verify(contentPostgresRepository).findAllByMovementIds(
			projectId,
			listOf(uuid),
		)
		verify(mapper, never()).toModel(any())
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
				this.projectId = fr.laucoin.registry.backend.test.ModelExt.projectId
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
				this.projectId = fr.laucoin.registry.backend.test.ModelExt.projectId
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
			verify(postgresRepository).findById(projectId, uuid, isVisible = null)
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
				this.projectId = fr.laucoin.registry.backend.test.ModelExt.projectId
				content = emptyList()
				create(currentUser())
			}

			// Act
			val result = repository.update(movement).block()

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(postgresRepository).findById(projectId, uuid, isVisible = null)
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
