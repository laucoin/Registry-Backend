package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileRoleEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IProjectProfileEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectProfileId
import fr.laucoin.registry.backend.test.ModelExt.defaultUserIdWithoutProfile
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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectProfileModelPostgresRepositoryTest : TestContext() {
	@MockitoSpyBean
	private lateinit var postgresRepository: IProjectProfileEntityRepository

	@MockitoSpyBean
	private lateinit var mapper: ProjectProfileEntityMapper

	@MockitoSpyBean
	private lateinit var roleMapper: ProjectProfileRoleEntityMapper

	@MockitoSpyBean
	private lateinit var roleCountMapper: ProjectProfileRoleCountEntityMapper

	@Autowired
	private lateinit var repository: IProjectProfilePort

	@Test
	fun `Should findProjectProfilesPageByUserId call repository findByUserId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(status = null)

		// Act
		val result = repository.findProjectProfilesPageByUserId(currentUser().id!!, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(1, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findByUserId(
			currentUser().id!!,
			query = null,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			at = null,
			role = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, times(1)).toModel(any())
	}

	@Test
	fun `Should findProjectProfilesPageByProjectId keep only the profiles with the requested role`() {
		// Arrange
		val pageable = PageableModel(0, 200)
		val all = repository.findProjectProfilesPageByProjectId(
			defaultProjectId, pageable, ProjectProfileSearchParamModel(status = null),
		).block()!!.content
		val role = all.mapNotNull { it.role }.first()

		// Act
		val result = repository.findProjectProfilesPageByProjectId(
			defaultProjectId, pageable, ProjectProfileSearchParamModel(status = null, role = role),
		).block()!!

		// Assert
		assertEquals(all.count { it.role == role }, result.content.size)
		assertTrue(result.content.all { it.role == role })
	}

	@Test
	fun `Should findProjectProfilesPageByProjectId call repository findByProjectId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ProjectProfileSearchParamModel(status = null)

		// Act
		val result = repository.findProjectProfilesPageByProjectId(defaultProjectId, pageable, params).block()

		// Assert
		assertNotNull(result)
		assertEquals(0, result.page)
		assertEquals(10, result.size)
		assertEquals(4, result.totalElements)
		assertEquals(1, result.totalPages)
		verify(postgresRepository).findByProjectId(
			defaultProjectId,
			query = null,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			at = null,
			role = null,
			sortField = null,
			isDescending = null,
			pageable.limit,
			pageable.offset,
		)
		verify(mapper, atLeastOnce()).toModel(any())
	}

	@Test
	fun `Should findUserIdsWithProjectProfileForProjectWithProfileExclusion call repository findUserIdsWithProjectProfileForProjectWithProfileExclusion`() {
		// Act
		val result = repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			defaultProjectId,
			listOf(currentUser().id!!),
			profileIdToExclude = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			from = null,
			to = null
		).collectList().block()

		// Assert
		assertEquals(1, result?.size)
		verify(postgresRepository).findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			defaultProjectId,
			listOf(currentUser().id!!),
			profileIdToExclude = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
			from = null,
			to = null
		)
	}

	@Test
	fun `Should findProjectProfilesRolesByUserId call repository findAllRolesByUserId with visible profiles only`() {
		// Act
		val result = repository.findProjectProfilesRolesByUserId(currentUser().id!!)
			.collectList()
			.block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findAllRolesByUserId(
			currentUser().id!!,
			isVisible = true,
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
			defaultProjectProfileId,
			isVisible = null
		).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findByUserIdAndId(
			currentUser().id!!,
			defaultProjectProfileId,
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
		val result = repository.findById(defaultProjectId, defaultProjectProfileId, isVisible = null).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findByProjectIdAndId(
			defaultProjectId,
			defaultProjectProfileId,
			isVisible = null,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findById call repository findByProjectIdAndId and return null`() {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = repository.findById(defaultProjectId, uuid, isVisible = null).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findByProjectIdAndId(
			defaultProjectId,
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
		val result =
			repository.findProjectProfileByProjectAndUserId(defaultProjectId, currentUser().id!!, params).block()

		// Assert
		assertNotNull(result)
		verify(postgresRepository).findProjectProfileByProjectAndUserId(
			defaultProjectId,
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
		val result =
			repository.findProjectProfileByProjectAndUserId(defaultProjectId, uuid, searchParams = params).block()

		// Assert
		assertNull(result)
		verify(postgresRepository).findProjectProfileByProjectAndUserId(
			defaultProjectId,
			uuid,
			isVisible = null,
			isAvailable = null,
			status = listOf(INVITED, ACCEPTED, REJECTED, BLOCKED),
		)
		verify(mapper, never()).toModel(any())
	}

	@Test
	fun `Should findProjectProfilesByUserIdAndProjectIds call repository findByUserIdAndProjectIds`() {
		// Arrange
		val params = ProjectProfileSearchParamModel(isVisible = null)
		val projectIds = listOf(defaultProjectId, UUID.randomUUID())

		// Act
		val result = repository.findProjectProfilesByUserIdAndProjectIds(currentUser().id!!, projectIds, params)
			.collectList().block()

		// Assert
		assertNotNull(result)
		assertEquals(1, result.size)
		assertEquals(defaultProjectId, result.first().projectId)
		assertNull(result.first().project)
		verify(postgresRepository).findByUserIdAndProjectIds(
			currentUser().id!!,
			projectIds,
			isVisible = null,
			isAvailable = null,
			status = params.status,
		)
		verify(mapper).toModel(any())
	}

	@Test
	fun `Should findProjectProfilesByUserIdAndProjectIds return nothing without calling repository when no project`() {
		// Act
		val result = repository.findProjectProfilesByUserIdAndProjectIds(
			currentUser().id!!,
			emptyList(),
			ProjectProfileSearchParamModel(isVisible = null)
		).collectList().block()

		// Assert
		assertEquals(emptyList(), result)
		verify(postgresRepository, never()).findByUserIdAndProjectIds(any(), any(), anyOrNull(), anyOrNull(), any())
	}

	@Test
	fun `Should findProjectProfilesByUserIdAndProjectIds not return the profile of an unknown user`() {
		// Act
		val result = repository.findProjectProfilesByUserIdAndProjectIds(
			UUID.randomUUID(),
			listOf(defaultProjectId),
			ProjectProfileSearchParamModel(isVisible = null)
		).collectList().block()

		// Assert
		assertEquals(emptyList(), result)
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
	fun `Should lockLevel0ProjectProfilesByUserId call repository lockLevel0ProjectProfilesByUserId`() {
		// Act
		val result = repository.lockLevel0ProjectProfilesByUserId(currentUser().id!!).collectList().block()

		// Assert
		assertFalse(result.isNullOrEmpty())
		verify(postgresRepository).lockLevel0ProjectProfilesByUserId(currentUser().id!!)
	}

	@Test
	fun `Should findLevel0ProjectProfileRoleByProjectId call repository findLevel0ProjectProfileRoleByProjectId`() {
		// Act
		val result =
			repository.findLevel0ProjectProfileRoleByProjectId(defaultProjectId, isVisible = null).collectList()
				.block()

		// Assert
		assertFalse(result.isNullOrEmpty())
		verify(postgresRepository).findLevel0ProjectProfileRoleByProjectId(
			defaultProjectId,
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
				user = UserModel().apply { id = defaultUserIdWithoutProfile }
				this.projectId = defaultProjectId
				role = "PROJECT_ADMINISTRATOR"
				status = INVITED
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
			}

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
				user = UserModel().apply { id = defaultUserIdWithoutProfile }
				this.projectId = defaultProjectId
				role = "PROJECT_ADMINISTRATOR"
				status = ACCEPTED
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
			}

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
				user = UserModel().apply { id = defaultUserIdWithoutProfile }
				this.projectId = defaultProjectId
				role = "PROJECT_ADMINISTRATOR"
				status = INVITED
				create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
			}

			// Act
			val result = repository.saveAll(listOf(projectProfile)).collectList().block()

			// Assert
			assertFalse(result.isNullOrEmpty())
			verify(mapper).toEntity(any())
			verify(mapper).toModel(any())
		}
	}
}
