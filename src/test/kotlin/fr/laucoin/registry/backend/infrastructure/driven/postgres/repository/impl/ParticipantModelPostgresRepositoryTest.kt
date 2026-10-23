package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IParticipantEntityRepository
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
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParticipantModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IParticipantEntityRepository

	@MockitoSpyBean
	private lateinit var contentPostgresRepository: IGroupContentEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: ParticipantEntityMapper

	@Autowired
	private lateinit var repository: IParticipantPort

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

	@ParameterizedTest
	@ValueSource(booleans = [true, false])
	fun `Should findPage filter on adulthood`(isMajor: Boolean) {
		// Arrange
		val pageable = PageableModel(0, 200)
		val eighteenYearsAgo = LocalDate.now().minusYears(18)
		val everyone =
			repository.findPage(defaultProjectId, pageable, ParticipantSearchParamModel(query = null, at = null))
				.block()!!.content
		val expectedCount = everyone.count { isMajor == !it.birthday!!.isAfter(eighteenYearsAgo) }

		// Act
		val result =
			repository.findPage(defaultProjectId, pageable, ParticipantSearchParamModel(isMajor = isMajor)).block()

		// Assert
		assertNotNull(result)
		assertEquals(expectedCount.toLong(), result.totalElements)
		result.content.forEach {
			val birthday = assertNotNull(it.birthday)
			assertEquals(isMajor, !birthday.isAfter(eighteenYearsAgo))
		}
	}

	@Test
	fun `Should findUnusedSince only select participants created before the threshold`() {
		// Act & Assert
		assertTrue(postgresRepository.findUnusedSince(LocalDate.EPOCH).collectList().block()!!.isEmpty())
		assertTrue(
			postgresRepository.findUnusedSince(LocalDate.now().plusYears(1)).collectList().block()!!.isNotEmpty()
		)
	}

	@Test
	fun `Should findPage keep the close matches and drop the unrelated ones`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val reference =
			repository.findPage(defaultProjectId, pageable, ParticipantSearchParamModel(query = null, at = null))
				.block()!!.content.first()

		// Act
		val close = repository.findPage(
			defaultProjectId, pageable, ParticipantSearchParamModel(query = reference.lastName, at = null),
		).block()!!
		val unrelated = repository.findPage(
			defaultProjectId, pageable, ParticipantSearchParamModel(query = "zzqxjv", at = null),
		).block()!!

		// Assert
		assertTrue(close.content.any { it.id == reference.id })
		assertEquals(0, unrelated.content.size)
	}

	@Test
	fun `Should findPage expose the effective dates backfilled from the seed movements`() {
		// Arrange
		val pageable = PageableModel(0, 200)

		// Act
		val participants =
			repository.findPage(defaultProjectId, pageable, ParticipantSearchParamModel(query = null, at = null))
				.block()!!.content

		// Assert
		assertTrue(participants.any { it.effectiveArrival != null })
		assertTrue(participants.filter { it.effectiveArrival != null }
			.all { !it.effectiveArrival!!.isAfter(it.lastMovement) })
		assertTrue(participants.filter { it.type == REGISTERED }.all { it.effectiveDeparture == null })
		assertTrue(participants.filter { it.type == GUEST && it.effectiveDeparture != null }
			.all { it.effectiveDeparture == it.lastMovement })
	}

	@Test
	fun `Should findPage split the participants by account link`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val total =
			repository.findPage(defaultProjectId, pageable, ParticipantSearchParamModel(query = null, at = null))
				.block()!!.totalElements

		// Act
		val linked = repository.findPage(
			defaultProjectId,
			pageable,
			ParticipantSearchParamModel(query = null, at = null, hasUser = true)
		).block()!!
		val unlinked = repository.findPage(
			defaultProjectId,
			pageable,
			ParticipantSearchParamModel(query = null, at = null, hasUser = false)
		).block()!!

		// Assert
		assertTrue(linked.content.all { it.user != null })
		assertTrue(unlinked.content.all { it.user == null })
		assertEquals(total, linked.totalElements + unlinked.totalElements)
	}

	@Test
	fun `Should findPage keep the birthdays inside the requested period`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val from = LocalDate.of(2000, 1, 1)
		val to = LocalDate.of(2010, 12, 31)

		// Act
		val result = repository.findPage(
			defaultProjectId,
			pageable,
			ParticipantSearchParamModel(query = null, at = null, birthdayFrom = from, birthdayTo = to),
		).block()!!

		// Assert
		assertTrue(result.content.isNotEmpty())
		assertTrue(result.content.all {
			it.birthday != null && !it.birthday!!.isBefore(from) && !it.birthday!!.isAfter(
				to
			)
		})
	}

	@Test
	fun `Should findPage call repository findAll`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ParticipantSearchParamModel()

		// Act
		val result = repository.findPage(defaultProjectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(50, result.totalElements)
		assertEquals(5, result.totalPages)
		verify(postgresRepository).findAll(
			defaultProjectId,
			query = null,
			isMajor = null,
			type = null,
			isVisible = null,
			isAvailable = null,
			status = null,
			presence = null,
			at = null,
			hasUser = null,
			isOnSite = null,
			birthdayFrom = null,
			birthdayTo = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, times(10)).toModel(any())
	}

	@Test
	fun `Should findPageByGroupId call repository findAllByGroupId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ParticipantSearchParamModel()

		// Act
		val result = repository.findPageByGroupId(defaultProjectId, defaultGroupId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(3, result.totalPages)
		verify(postgresRepository).findAllByGroupId(
			defaultProjectId,
			defaultGroupId,
			query = null,
			isMajor = null,
			type = null,
			isVisible = null,
			isAvailable = null,
			status = null,
			presence = null,
			at = null,
			hasUser = null,
			isOnSite = null,
			birthdayFrom = null,
			birthdayTo = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
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
		val result = repository.findAllByIds(defaultProjectId, ids, isVisible = null).collectList().block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository, times(expectedDatabaseCall)).findAllByIds(
			defaultProjectId,
			ids,
			isVisible = null,
			at = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findByUserId call repository findByUserId`() {
		// Act
		repository.findByUserId(defaultProjectId, currentUser().id!!).collectList().block()

		// Assert
		verify(postgresRepository).findByUserId(defaultProjectId, currentUser().id!!, at = null)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findAllByUserId call repository findAllByUserId`() {
		// Act
		val result = repository.findAllByUserId(currentUser().id!!).collectList().block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findAllByUserId(currentUser().id!!)
		verify(mapper, times(result.size)).toModel(any())
	}

	@Test
	fun `Should findWithLimit call repository findWithLimit`() {
		// Arrange
		val size = 10
		val params = ParticipantSearchParamModel()

		// Act
		val result = repository.findWithLimit(size, defaultProjectId, params).collectList().block()

		// Assert
		assertNotNull(result)
		assertEquals(10, result.size)
		verify(postgresRepository).findWithLimit(
			defaultProjectId,
			query = null,
			isMajor = null,
			type = null,
			isVisible = null,
			isAvailable = null,
			status = null,
			presence = null,
			at = null,
			size,
		)
		verify(mapper, times(10)).toModel(any())
	}

	@Test
	fun `Should findById call repository findById`() {
		// Act
		val result = repository.findById(defaultProjectId, defaultParticipantId, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findById(
			defaultProjectId,
			defaultParticipantId,
			isVisible = null,
			at = null,
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
			at = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Nested
	@TestInstance(PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
	inner class BirthdayTests {
		private lateinit var matchingId: UUID
		private lateinit var nonMatchingId: UUID

		@Test
		@Order(1)
		fun `Should create a participant born today's month-day in a past year and one born on another day`() {
			// Arrange
			val bornToday = ParticipantModel().apply {
				firstName = "birthday"
				lastName = "match"
				birthday = LocalDate.now().minusYears(30)
				type = REGISTERED
				this.projectId = defaultProjectId
				create(currentUser())
			}
			val bornAnotherDay = ParticipantModel().apply {
				firstName = "birthday"
				lastName = "no-match"
				birthday = LocalDate.now().minusYears(30).minusDays(1)
				type = REGISTERED
				this.projectId = defaultProjectId
				create(currentUser())
			}

			// Act
			matchingId = repository.create(bornToday).block()!!.id!!
			nonMatchingId = repository.create(bornAnotherDay).block()!!.id!!

			// Assert
			assertNotNull(matchingId)
			assertNotNull(nonMatchingId)
		}

		@Test
		@Order(2)
		fun `Should findBirthdays match on month-day regardless of birth year`() {
			// Act
			val result = repository.findBirthdays(defaultProjectId, isVisible = null).collectList().block()!!

			// Assert
			assertTrue(result.any { it.id == matchingId })
			assertFalse(result.any { it.id == nonMatchingId })
		}

		@Test
		@Order(3)
		fun `Should deleteById clean up the birthday test participants`() {
			// Act
			repository.deleteById(matchingId).block()
			repository.deleteById(nonMatchingId).block()

			// Assert
			assertNull(repository.findById(defaultProjectId, matchingId, isVisible = null).block())
			assertNull(repository.findById(defaultProjectId, nonMatchingId, isVisible = null).block())
		}
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
			val participant = ParticipantModel().apply {
				firstName = "test"
				lastName = "test"
				birthday = LocalDate.EPOCH
				type = REGISTERED
				this.projectId = defaultProjectId
				create(currentUser())
			}

			// Act
			val result = repository.create(participant).block()
			uuid = result!!.id!!

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(mapper).toEntity(any())
			verify(mapper, times(2)).toModel(any())
		}

		@Test
		@Order(2)
		fun `Should update call repository save and add group for participant`() {
			// Arrange
			val participant = ParticipantModel().apply {
				id = uuid
				firstName = "test updated"
				lastName = "test updated"
				birthday = LocalDate.EPOCH
				type = REGISTERED
				groups = listOf(GroupModel().apply { id = defaultGroupId })
				this.projectId = defaultProjectId
				create(currentUser())
			}

			// Act
			val result = repository.update(participant).block()

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(postgresRepository, times(2)).findById(defaultProjectId, uuid, isVisible = null, at = null)
			verify(contentPostgresRepository).saveAll(any<Iterable<GroupContentEntity>>())
			verify(mapper).toEntity(any())
			verify(mapper, atLeastOnce()).toModel(any())
		}

		@Test
		@Order(3)
		fun `Should update call repository save and remove group from participant`() {
			// Arrange
			val participant = ParticipantModel().apply {
				id = uuid
				firstName = "test updated"
				lastName = "test updated"
				birthday = LocalDate.EPOCH
				type = REGISTERED
				this.projectId = defaultProjectId
				create(currentUser())
			}

			// Act
			val result = repository.update(participant).block()

			// Assert
			assertNotNull(result)
			verify(postgresRepository).save(any())
			verify(postgresRepository, times(2)).findById(defaultProjectId, uuid, isVisible = null, at = null)
			verify(contentPostgresRepository).deleteAllByParticipantIdAndGroupIds(uuid, listOf(defaultGroupId))
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
