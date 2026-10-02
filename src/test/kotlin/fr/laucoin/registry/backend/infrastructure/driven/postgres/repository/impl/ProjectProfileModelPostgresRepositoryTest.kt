package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.RESOLVED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ALERT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_PROFILE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.ProjectProfileJooqRepository
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.projectProfileId
import fr.laucoin.registry.backend.test.ModelExt.userIdWithoutProfile
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jooq.DSLContext
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import reactor.core.publisher.Mono

class ProjectProfileModelPostgresRepositoryTest: TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: ProjectProfileJooqRepository

	@MockitoSpyBean
	private lateinit var mapper: ProjectProfileEntityMapper

	@MockitoSpyBean
	private lateinit var roleMapper: ProjectProfileRoleEntityMapper

	@MockitoSpyBean
	private lateinit var roleCountMapper: ProjectProfileRoleCountEntityMapper

	@Autowired
	private lateinit var repository: IProjectProfilePort

	@Autowired
	private lateinit var dsl: DSLContext

	@Test
	fun `Should findProjectProfilesPageByUserId call repository findByUserId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(status = null)

		// Act
		val result = repository.findProjectProfilesPageByUserId(currentUser().id!!, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.pageNumber)
		assertEquals(10, result.pageSize)
		assertEquals(1, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findByUserId(
			currentUser().id!!,
			query = null,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			dateTime = null,
			isFavorite = null,
			isUpcoming = null,
			sortFields = emptyList(),
			limit = pageable.limit,
			offset = pageable.offset,
			includeCounts = false,
		)
		verify(mapper, times(1)).toModel(any())
	}

	@Test
	fun `Should findProjectProfilesPageByUserId forward includeCounts to the port`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(status = null)

		// Act
		val result = repository.findProjectProfilesPageByUserId(
			currentUser().id!!, pageable, params, includeCounts = true
		).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findByUserId(
			currentUser().id!!,
			query = null,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			dateTime = null,
			isFavorite = null,
			isUpcoming = null,
			sortFields = emptyList(),
			limit = pageable.limit,
			offset = pageable.offset,
			includeCounts = true,
		)
	}

	@Test
	fun `Should findProjectsRequiringAttentionByUserId only return ALERT-enabled Projects with an ongoing Alert, sorted and limited`() {
		// Arrange: two dedicated Projects (the ALERT option enabled, an ACCEPTED Profile for the current
		// user, and a different number of IN_PROGRESS Alerts each) plus a RESOLVED Alert as a decoy.
		val busyProjectId = UUID.randomUUID()
		val busierProjectId = UUID.randomUUID()
		val busyProfileId = UUID.randomUUID()
		val busierProfileId = UUID.randomUUID()

		createAlertEnabledProject(busyProjectId)
		createAlertEnabledProject(busierProjectId)
		createAcceptedProfile(busyProfileId, busyProjectId)
		createAcceptedProfile(busierProfileId, busierProjectId)
		createAlerts(busyProjectId, IN_PROGRESS, count = 3)
		createAlerts(busyProjectId, RESOLVED, count = 1)
		createAlerts(busierProjectId, IN_PROGRESS, count = 5)

		try {
			// Act
			val all = repository.findProjectsRequiringAttentionByUserId(currentUser().id!!, limit = 10).collectList().block()!!

			// Assert: sorted by ongoingAlerts descending, and the pre-existing seeded Project — whose
			// ALERT option is disabled despite having IN_PROGRESS Alerts — is correctly excluded.
			assertEquals(listOf(busierProjectId, busyProjectId), all.map { it.id })
			assertEquals(5L, all[0].counts?.ongoingAlerts)
			assertEquals(3L, all[1].counts?.ongoingAlerts)
			assertTrue(all.none { it.id == projectId })
			verify(postgresRepository).findAcceptedByUserId(currentUser().id!!, limit = 10)

			// Act: the limit is enforced in SQL, keeping only the highest-ranked Project
			val limited =
				repository.findProjectsRequiringAttentionByUserId(currentUser().id!!, limit = 1).collectList().block()!!

			// Assert
			assertEquals(listOf(busierProjectId), limited.map { it.id })
		} finally {
			Mono.from(dsl.deleteFrom(TB_ALERT).where(TB_ALERT.PROJECT_ID.`in`(busyProjectId, busierProjectId))).block()
			Mono.from(dsl.deleteFrom(TB_PROJECT_PROFILE).where(TB_PROJECT_PROFILE.ID.`in`(busyProfileId, busierProfileId))).block()
			Mono.from(dsl.deleteFrom(TB_PROJECT).where(TB_PROJECT.ID.`in`(busyProjectId, busierProjectId))).block()
		}
	}

	private fun createAlertEnabledProject(id: UUID) {
		Mono.from(
			dsl.insertInto(TB_PROJECT, TB_PROJECT.ID, TB_PROJECT.NAME, TB_PROJECT.OPTIONS)
				.values(id, "Counts test project $id", listOf(ProjectOptionEnum.ALERT))
		).block()
	}

	private fun createAcceptedProfile(id: UUID, forProjectId: UUID) {
		Mono.from(
			dsl.insertInto(
				TB_PROJECT_PROFILE,
				TB_PROJECT_PROFILE.ID,
				TB_PROJECT_PROFILE.USER_ID,
				TB_PROJECT_PROFILE.PROJECT_ID,
				TB_PROJECT_PROFILE.ROLE,
				TB_PROJECT_PROFILE.STATUS,
			).values(id, currentUser().id!!, forProjectId, "PROJECT_ADMINISTRATOR", ACCEPTED)
		).block()
	}

	private fun createAlerts(forProjectId: UUID, status: AlertStatusEnum, count: Int) {
		var insert = dsl.insertInto(TB_ALERT, TB_ALERT.PROJECT_ID, TB_ALERT.STATUS)
		repeat(count) { insert = insert.values(forProjectId, status) }
		Mono.from(insert).block()
	}

	@Test
	fun `Should findProjectProfilesPageByProjectId call repository findByProjectId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(status = null)

		// Act
		val result = repository.findProjectProfilesPageByProjectId(projectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.pageNumber)
		assertEquals(10, result.pageSize)
		assertEquals(4, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findByProjectId(
			projectId,
			query = null,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			dateTime = null,
			sortFields = emptyList(),
			limit = pageable.limit,
			offset = pageable.offset,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findUserIdsWithProjectProfileForProjectWithProfileExclusion call repository findUserIdsWithProjectProfileForProjectWithProfileExclusion`() {
		// Act
		val result = repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			listOf(currentUser().id!!),
			profileIdToExclude = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			startDateTime = null,
			endDateTime = null
		).collectList().block()

		// Assert
		assertEquals(1, result?.size)
		verify(postgresRepository).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			listOf(currentUser().id!!),
			profileIdToExclude = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			startDateTime = null,
			endDateTime = null
		)
	}

	@Test
	fun `Should findProjectProfilesRolesByUserId call repository findAllRolesByUserId`() {
		// Act
		val result = repository.findProjectProfilesRolesByUserId(currentUser().id!!)
			.collectList()
			.block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findAllRolesByUserId(
			currentUser().id!!,
			isVisible = null,
			isAvailable = true,
			status = listOf(ACCEPTED),
		)
		verify(roleMapper).toModel(any())
	}

	@Test
	fun `Should findProjectProfileByUserIdAndId call repository findByUserIdAndId`() {
		// Act
		val result = repository.findProjectProfileByUserIdAndId(
			currentUser().id!!,
			projectProfileId,
			isVisible = null
		).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findByUserIdAndId(
			currentUser().id!!,
			projectProfileId,
			isVisible = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findProjectProfileByUserIdAndId call repository findByUserIdAndId and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result =
			repository.findProjectProfileByUserIdAndId(currentUser().id!!, uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findByUserIdAndId(
			currentUser().id!!,
			uuid,
			isVisible = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findById call repository findByProjectIdAndId`() {
		// Act
		val result = repository.findById(projectId, projectProfileId, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findByProjectIdAndId(
			projectId,
			projectProfileId,
			isVisible = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findById call repository findByProjectIdAndId and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = repository.findById(projectId, uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findByProjectIdAndId(
			projectId,
			uuid,
			isVisible = null,
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findProjectProfileByProjectAndUserId call repository findUsableProfileByProjectAndUserId`() {
		// Arrange
		val params = ProjectProfileSearchParamModel(status = null)

		// Act
		val result = repository.findProjectProfileByProjectAndUserId(projectId, currentUser().id!!, params).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findProjectProfileByProjectAndUserId(
			projectId,
			currentUser().id!!,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findProjectProfileByProjectAndUserId call repository findUsableProfileByProjectAndUserId and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val params = ProjectProfileSearchParamModel(status = null)

		// Act
		val result = repository.findProjectProfileByProjectAndUserId(projectId, uuid, searchParams = params).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findProjectProfileByProjectAndUserId(
			projectId,
			uuid,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findLevel0ProjectProfileRoleByUserId call repository findLevel0ProjectProfileRoleByUserId`() {
		// Act
		val result =
			repository.findLevel0ProjectProfileRoleByUserId(currentUser().id!!, isVisible = null).collectList()
				.block()

		// Assert
		assertFalse(result.isNullOrEmpty())
		verify(postgresRepository).findLevel0ProjectProfileRoleByUserId(
			currentUser().id!!,
			isVisible = null,
		)
		verify(roleCountMapper).toModel(any())
	}

	@Test
	fun `Should findLevel0ProjectProfileRoleByProjectId call repository findLevel0ProjectProfileRoleByProjectId`() {
		// Act
		val result =
			repository.findLevel0ProjectProfileRoleByProjectId(projectId, isVisible = null).collectList()
				.block()

		// Assert
		assertFalse(result.isNullOrEmpty())
		verify(postgresRepository).findLevel0ProjectProfileRoleByProjectId(
			projectId,
			isVisible = null,
		)
		verify(mapper, atLeastOnce()).toModel(any())
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
			val projectProfile = ProjectProfileModel().apply {
				user = UserModel().apply { id = userIdWithoutProfile }
				role = "PROJECT_ADMINISTRATOR"
				status = INVITED
				create(currentUser())
			}.also { it.projectId = projectId }

			// Act
			val result = repository.create(projectProfile).block()
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
			val projectProfile = ProjectProfileModel().apply {
				user = UserModel().apply { id = userIdWithoutProfile }
				role = "PROJECT_ADMINISTRATOR"
				status = ACCEPTED
				create(currentUser())
			}.also { it.projectId = projectId }

			// Act
			val result = repository.update(projectProfile).block()

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

		@Test
		@Order(4)
		fun `Should saveAll call repository saveAll`() {
			// Arrange
			val projectProfile = ProjectProfileModel().apply {
				user = UserModel().apply { id = userIdWithoutProfile }
				role = "PROJECT_ADMINISTRATOR"
				status = INVITED
				create(currentUser())
			}.also { it.projectId = projectId }

			// Act
			val result = repository.saveAll(listOf(projectProfile)).collectList().block()

			// Assert
			assertFalse(result.isNullOrEmpty())
			verify(mapper).toEntity(any())
			verify(mapper).toModel(any())
		}
	}
}
