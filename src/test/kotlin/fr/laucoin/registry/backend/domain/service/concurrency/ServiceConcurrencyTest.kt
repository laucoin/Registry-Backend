package fr.laucoin.registry.backend.domain.service.concurrency

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Deux requêtes simultanées contre la vraie base : chaque invariant protégé par un verrou ne doit laisser passer qu'une seule d'entre elles.
 */
class ServiceConcurrencyTest : TestContext() {
	@Autowired
	private lateinit var userService: IUserService

	@Autowired
	private lateinit var participantService: IParticipantService

	@Autowired
	private lateinit var profileService: IProjectProfileService

	@Autowired
	private lateinit var activityService: IActivityService

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var transactionalOperator: TransactionalOperator

	private var demotedAdministrators: List<UUID> = emptyList()

	@BeforeEach
	fun isolateAdministrators() {
		demotedAdministrators = databaseClient.sql(
			"UPDATE tb_user SET role = 'USER' WHERE role = 'USER_ADMINISTRATOR' AND type <> 'SERVICE_ACCOUNT' RETURNING id"
		).map { row -> row.get("id", UUID::class.java)!! }.all().collectList().block()!!
	}

	@AfterEach
	fun restoreAdministrators() {
		databaseClient.sql("UPDATE tb_user SET role = 'USER' WHERE role = 'USER_ADMINISTRATOR' AND type <> 'SERVICE_ACCOUNT'")
			.fetch().rowsUpdated().block()
		if (demotedAdministrators.isNotEmpty()) {
			databaseClient.sql("UPDATE tb_user SET role = 'USER_ADMINISTRATOR' WHERE id IN (:ids)")
				.bind("ids", demotedAdministrators).fetch().rowsUpdated().block()
		}
	}

	@Test
	fun `Should two administrators blocking each other concurrently never leave the platform without administrator`() {
		// Arrange
		val first = insertUser(role = "USER_ADMINISTRATOR")
		val second = insertUser(role = "USER_ADMINISTRATOR")

		// Act
		val outcomes = concurrently(
			userService.blockUserById(administrator(first), second),
			userService.blockUserById(administrator(second), first),
		)

		// Assert
		assertEquals(1, outcomes.count { it.isSuccess })
		val failure = outcomes.single { it.isFailure }.exceptionOrNull() as RegistryException
		assertEquals(USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR, failure.code)
		assertEquals(1, countVisibleAdministrators())
	}

	@Test
	fun `Should two participants linked to the same user concurrently be refused for the second one`() {
		// Arrange
		val projectId = insertProject()
		val userId = insertUser(role = "USER")

		// Act
		val outcomes = concurrently(
			participantService.createParticipant(administrator(userId), participantLinkedTo(projectId, userId, "First")),
			participantService.createParticipant(administrator(userId), participantLinkedTo(projectId, userId, "Second")),
		)

		// Assert
		assertEquals(1, outcomes.count { it.isSuccess })
		val failure = outcomes.single { it.isFailure }.exceptionOrNull() as RegistryException
		assertEquals(PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER, failure.code)
		assertEquals(1L, count("SELECT COUNT(*) FROM tb_participant WHERE project_id = :id", projectId))
	}

	@Test
	fun `Should two invitations of the same user on the same range concurrently create a single profile`() {
		// Arrange
		val projectId = insertProject()
		val userId = insertUser(role = "USER")
		val invite = {
			profileService.createProjectProfiles(administrator(userId), projectId, listOf(userId), listOf(invitation(projectId, userId)))
		}

		// Act
		val outcomes = concurrently(invite(), invite())

		// Assert
		assertEquals(1, outcomes.count { it.isSuccess })
		val failure = outcomes.single { it.isFailure }.exceptionOrNull() as RegistryException
		assertEquals(PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE, failure.code)
		assertEquals(1L, count("SELECT COUNT(*) FROM tb_project_profile WHERE project_id = :id", projectId))
	}

	@Test
	fun `Should deleting an activity wait for a movement being created on it and then refuse`() {
		// Arrange
		val projectId = insertProject()
		val activityId = insertActivity(projectId)
		val movementId = UUID.randomUUID()
		val inserted = Sinks.one<Boolean>()
		val release = Sinks.empty<Void>()
		val creation = databaseClient.sql("INSERT INTO tb_movement(id, type, project_id, activity_id) VALUES (:id, 'IN', :project, :activity)")
			.bind("id", movementId).bind("project", projectId).bind("activity", activityId)
			.fetch().rowsUpdated()
			.then(Mono.fromRunnable<Void> { inserted.tryEmitValue(true) })
			.then(release.asMono())
			.`as`(transactionalOperator::transactional)
			.subscribe()
		inserted.asMono().block(Duration.ofSeconds(10))

		// Act
		val deletion = activityService.deleteActivityById(administrator(UUID.randomUUID()), projectId, activityId)
			.materialize().subscribeOn(Schedulers.parallel()).toFuture()
		Thread.sleep(PENDING_DELETION_MILLIS)
		release.tryEmitEmpty()
		val signal = deletion.get(TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)!!
		creation.dispose()

		// Assert
		val error = signal.throwable as RegistryException
		assertEquals(ACTIVITY_DELETE_HAS_MOVEMENT, error.code)
		assertEquals(1L, count("SELECT COUNT(*) FROM tb_activity WHERE id = :id", activityId))
		assertEquals(1L, count("SELECT COUNT(*) FROM tb_movement WHERE id = :id", movementId))
	}

	private fun <T : Any> concurrently(vararg actions: Mono<T>): List<Result<T>> {
		val futures = actions.map { action ->
			action.map { Result.success(it) }
				.onErrorResume { Mono.just(Result.failure<T>(it)) }
				.subscribeOn(Schedulers.parallel())
				.toFuture()
		}
		return futures.map { it.get(TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)!! }
	}

	private fun administrator(id: UUID) = CurrentUserModel().apply { this.id = id; role = "USER_ADMINISTRATOR" }

	private fun participantLinkedTo(projectId: UUID, userId: UUID, firstName: String) = ParticipantModel(
		firstName = firstName,
		lastName = "Linked",
		birthday = LocalDate.of(1990, 1, 1),
		type = REGISTERED,
		user = UserModel().apply { id = userId },
	).apply { this.projectId = projectId }

	private fun invitation(projectId: UUID, userId: UUID) = ProjectProfileModel(
		user = UserModel().apply { id = userId },
		role = "PROJECT_PARTICIPANT",
	).apply { this.projectId = projectId }

	private fun insertUser(role: String): UUID {
		val id = UUID.randomUUID()
		databaseClient.sql(
			"INSERT INTO tb_user(id, oidc_id, type, first_name, last_name, email, role) VALUES (:id, :oidc, 'USER', 'Concurrent', 'User', :email, :role)"
		).bind("id", id).bind("oidc", UUID.randomUUID()).bind("email", "$id@concurrency.test").bind("role", role)
			.fetch().rowsUpdated().block()
		return id
	}

	private fun insertProject(): UUID {
		val id = UUID.randomUUID()
		databaseClient.sql("INSERT INTO tb_project(id, name) VALUES (:id, 'Concurrency')").bind("id", id).fetch().rowsUpdated().block()
		return id
	}

	private fun insertActivity(projectId: UUID): UUID {
		val id = UUID.randomUUID()
		databaseClient.sql("INSERT INTO tb_activity(id, name, project_id) VALUES (:id, 'Concurrency', :project)")
			.bind("id", id).bind("project", projectId).fetch().rowsUpdated().block()
		return id
	}

	private fun countVisibleAdministrators(): Long = databaseClient.sql(
		"SELECT COUNT(*) AS total FROM tb_user WHERE role = 'USER_ADMINISTRATOR' AND visible IS TRUE AND type <> 'SERVICE_ACCOUNT'"
	).map { row -> row.get("total", java.lang.Long::class.java)!!.toLong() }.one().block()!!

	private fun count(sql: String, id: UUID): Long = databaseClient.sql(sql).bind("id", id)
		.map { row -> row.get(0, java.lang.Long::class.java)!!.toLong() }.one().block()!!

	private companion object {
		val TIMEOUT: Duration = Duration.ofSeconds(30)
		const val PENDING_DELETION_MILLIS = 500L
	}
}
