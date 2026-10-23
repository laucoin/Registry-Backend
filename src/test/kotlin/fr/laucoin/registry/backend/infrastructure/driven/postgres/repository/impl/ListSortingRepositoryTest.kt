package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.alertId
import fr.laucoin.registry.backend.test.ModelExt.groupId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.userId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
import fr.laucoin.registry.backend.test.TestContext
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono

class ListSortingRepositoryTest : TestContext() {
	private data class SortCase(
		val name: String,
		val fields: Set<String>,
		val run: (SortModel?) -> Mono<out PageModel<*>>,
	)

	@Autowired private lateinit var activityPort: IActivityPort
	@Autowired private lateinit var alertPort: IAlertPort
	@Autowired private lateinit var communicationPort: ICommunicationPort
	@Autowired private lateinit var groupPort: IGroupPort
	@Autowired private lateinit var movementPort: IMovementPort
	@Autowired private lateinit var participantPort: IParticipantPort
	@Autowired private lateinit var projectPort: IProjectPort
	@Autowired private lateinit var projectProfilePort: IProjectProfilePort
	@Autowired private lateinit var userPort: IUserPort
	@Autowired private lateinit var vehiclePort: IVehiclePort

	private val pageable = PageableModel(0, 200)

	@TestFactory
	fun `Should accept every supported sort field in both directions`(): Stream<DynamicTest> {
		return cases().stream().flatMap { case -> case.fields.stream().flatMap { field -> directions(case, field) } }
	}

	private data class OrderCase(
		val name: String,
		val field: String,
		val run: (SortModel) -> Mono<out PageModel<*>>,
		val key: (Any) -> Comparable<Any>?,
	)

	@TestFactory
	fun `Should order the elements by their sort key in both directions`(): Stream<DynamicTest> {
		return orderCases().stream().flatMap { case -> Stream.of(false, true).map { isDescending -> orderTest(case, isDescending) } }
	}

	private fun orderTest(case: OrderCase, isDescending: Boolean): DynamicTest {
		return dynamicTest("${case.name} ordered by ${if (isDescending) "-" else ""}${case.field}") {
			// Arrange
			val sort = SortModel(case.field, isDescending)

			// Act
			val keys = case.run(sort).block()!!.content.map { element -> case.key(element!!) }

			// Assert
			val present = keys.filterNotNull()
			assertEquals(if (isDescending) present.sortedDescending() else present.sorted(), present)
			assertEquals(keys.filterNotNull().size, keys.indexOfLast { it != null } + 1)
		}
	}

	@Test
	fun `Should page a sort on a repeated key without repeating or skipping elements`() {
		// Arrange
		val sort = SortModel("type")
		val total = movementPort.findPage(projectId, pageable, MovementSearchParamModel(type = null, sort = sort)).block()!!
		val pages = (0 until Math.ceilDiv(total.totalElements.toInt(), 7)).map { PageableModel(it * 7, 7) }

		// Act
		val ids = pages.flatMap {
			movementPort.findPage(projectId, it, MovementSearchParamModel(type = null, sort = sort)).block()!!.content.map { movement -> movement.id }
		}

		// Assert
		assertEquals(total.totalElements.toInt(), ids.size)
		assertEquals(ids.size, ids.toSet().size)
	}

	@Suppress("UNCHECKED_CAST")
	private fun orderCases(): List<OrderCase> = listOf(
		OrderCase("alerts", "dateTime", { alertPort.findPage(projectId, pageable, AlertSearchParamModel(query = null, sort = it)) }) { (it as AlertModel).dateTime.toInstant() as Comparable<Any> },
		OrderCase("communications", "dateTime", { communicationPort.findPage(projectId, pageable, CommunicationSearchParamModel(query = null, sort = it)) }) { (it as CommunicationModel).dateTime.toInstant() as Comparable<Any> },
		OrderCase("movements", "dateTime", { movementPort.findPage(projectId, pageable, MovementSearchParamModel(type = null, sort = it)) }) { (it as MovementModel).dateTime.toInstant() as Comparable<Any> },
		OrderCase("participants", "birthday", { participantPort.findPage(projectId, pageable, ParticipantSearchParamModel(query = null, at = null, sort = it)) }) { (it as ParticipantModel).birthday as Comparable<Any>? },
		OrderCase("users", "birthday", { userPort.findPage(pageable, UserSearchParamModel(query = null, sort = it)) }) { (it as UserModel).birthday as Comparable<Any>? },
		OrderCase("users", "lastLogin", { userPort.findPage(pageable, UserSearchParamModel(query = null, sort = it)) }) { (it as UserModel).lastLogin?.toInstant() as Comparable<Any>? },
		OrderCase("activities", "minAllowedParticipants", { activityPort.findPage(projectId, pageable, ActivitySearchParamModel(query = null, sort = it)) }) { (it as ActivityModel).allowedParticipants?.lower as Comparable<Any>? },
		OrderCase("activities", "maxAllowedParticipants", { activityPort.findPage(projectId, pageable, ActivitySearchParamModel(query = null, sort = it)) }) { (it as ActivityModel).allowedParticipants?.upper as Comparable<Any>? },
	)

	private fun directions(case: SortCase, field: String): Stream<DynamicTest> = Stream.of(false, true).map { isDescending ->
		dynamicTest("${case.name} sorted by ${if (isDescending) "-" else ""}$field") {
			// Arrange
			val expected = case.run(null).block()!!

			// Act
			val result = case.run(SortModel(field, isDescending)).block()!!

			// Assert
			assertEquals(expected.totalElements, result.totalElements)
			assertEquals(expected.content.size, result.content.size)
		}
	}

	private fun cases(): List<SortCase> = alertCases() + communicationCases() + groupCases() + movementCases() +
		participantCases() + projectCases() + profileCases() + userCases() + vehicleCases()

	private fun alertCases() = listOf(
		SortCase("alerts", AlertSearchParamModel.SORT_FIELDS) {
			alertPort.findPage(projectId, pageable, AlertSearchParamModel(query = null, sort = it))
		},
	)

	private fun communicationCases() = listOf(
		SortCase("communications", CommunicationSearchParamModel.SORT_FIELDS) {
			communicationPort.findPage(projectId, pageable, CommunicationSearchParamModel(query = null, sort = it))
		},
		SortCase("movement communications", CommunicationSearchParamModel.SORT_FIELDS) {
			communicationPort.findPageByMovementId(projectId, movementId, pageable, CommunicationSearchParamModel(query = null, sort = it))
		},
		SortCase("alert communications", CommunicationSearchParamModel.SORT_FIELDS) {
			communicationPort.findPageByAlertId(projectId, alertId, pageable, CommunicationSearchParamModel(query = null, sort = it))
		},
	)

	private fun groupCases() = listOf(
		SortCase("groups", GroupSearchParamModel.SORT_FIELDS) {
			groupPort.findPage(projectId, pageable, GroupSearchParamModel(query = null, sort = it))
		},
	)

	private fun movementCases() = listOf(
		SortCase("movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPage(projectId, pageable, MovementSearchParamModel(type = null, sort = it))
		},
		SortCase("current movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findCurrentPage(projectId, pageable, MovementSearchParamModel(type = null, sort = it))
		},
		SortCase("participant movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPageByParticipantId(projectId, participantId, pageable, MovementSearchParamModel(type = null, sort = it))
		},
		SortCase("vehicle movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPageByVehicleId(projectId, vehicleId, pageable, MovementSearchParamModel(type = null, sort = it))
		},
		SortCase("activity movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPageByActivityId(projectId, activityId, pageable, MovementSearchParamModel(type = null, sort = it))
		},
	)

	private fun participantCases() = listOf(
		SortCase("participants", ParticipantSearchParamModel.SORT_FIELDS) {
			participantPort.findPage(projectId, pageable, ParticipantSearchParamModel(query = null, at = null, sort = it))
		},
		SortCase("group members", ParticipantSearchParamModel.SORT_FIELDS) {
			participantPort.findPageByGroupId(projectId, groupId, pageable, ParticipantSearchParamModel(query = null, at = null, sort = it))
		},
	)

	private fun projectCases() = listOf(
		SortCase("projects", ProjectSearchParamModel.SORT_FIELDS) {
			projectPort.findPage(pageable, ProjectSearchParamModel(query = null, sort = it))
		},
		SortCase("projects of a list of ids", ProjectSearchParamModel.SORT_FIELDS) {
			projectPort.findPage(listOf(projectId), pageable, ProjectSearchParamModel(query = null, sort = it))
		},
	)

	private fun profileCases() = listOf(
		SortCase("project profiles", ProjectProfileSearchParamModel.SORT_FIELDS) {
			projectProfilePort.findProjectProfilesPageByProjectId(projectId, pageable, ProjectProfileSearchParamModel(query = null, sort = it))
		},
		SortCase("user profiles", ProjectProfileSearchParamModel.PROJECT_SORT_FIELDS) {
			projectProfilePort.findProjectProfilesPageByUserId(userId, pageable, ProjectProfileSearchParamModel(query = null, sort = it))
		},
	)

	private fun userCases() = listOf(
		SortCase("users", UserSearchParamModel.SORT_FIELDS) {
			userPort.findPage(pageable, UserSearchParamModel(query = null, sort = it))
		},
	)

	private fun vehicleCases() = listOf(
		SortCase("vehicles", VehicleSearchParamModel.SORT_FIELDS) {
			vehiclePort.findPage(projectId, pageable, VehicleSearchParamModel(query = null, at = null, sort = it))
		},
	)
}
