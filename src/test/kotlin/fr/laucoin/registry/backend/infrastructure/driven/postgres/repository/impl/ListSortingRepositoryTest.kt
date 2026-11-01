package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
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
import fr.laucoin.registry.backend.test.ModelExt.defaultActivityId
import fr.laucoin.registry.backend.test.ModelExt.defaultAlertId
import fr.laucoin.registry.backend.test.ModelExt.defaultGroupId
import fr.laucoin.registry.backend.test.ModelExt.defaultMovementId
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipantId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.test.ModelExt.defaultVehicleId
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListSortingRepositoryTest : TestContext() {
	private data class SortCase(
		val name: String,
		val fields: Set<String>,
		val run: (SortModel?) -> Mono<out PageModel<*>>,
	)

	@Autowired
	private lateinit var activityPort: IActivityPort

	@Autowired
	private lateinit var alertPort: IAlertPort

	@Autowired
	private lateinit var communicationPort: ICommunicationPort

	@Autowired
	private lateinit var groupPort: IGroupPort

	@Autowired
	private lateinit var movementPort: IMovementPort

	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var projectPort: IProjectPort

	@Autowired
	private lateinit var projectProfilePort: IProjectProfilePort

	@Autowired
	private lateinit var userPort: IUserPort

	@Autowired
	private lateinit var vehiclePort: IVehiclePort

	private val pageable = PageableModel(0, 200)

	@TestFactory
	fun `Should accept every supported sort field in both directions`(): Stream<DynamicTest> {
		return cases().stream().flatMap { case -> case.fields.stream().flatMap { field -> directions(case, field) } }
	}

	private data class OrderCase(
		val name: String,
		val field: String,
		val run: (SortModel) -> Mono<out PageModel<*>>,
		val key: (Any) -> Comparable<*>?,
	)

	@TestFactory
	fun `Should order the elements by their sort key in both directions`(): Stream<DynamicTest> {
		return orderCases().stream()
			.flatMap { case -> Stream.of(false, true).map { isDescending -> orderTest(case, isDescending) } }
	}

	private fun orderTest(case: OrderCase, isDescending: Boolean): DynamicTest {
		return dynamicTest("${case.name} ordered by ${if (isDescending) "-" else ""}${case.field}") {
			// Arrange
			val sort = SortModel(case.field, isDescending)

			// Act
			val keys = case.run(sort).block()!!.content.map { element -> case.key(element!!) }

			// Assert
			@Suppress("UNCHECKED_CAST")
			val present = keys.filterNotNull() as List<Comparable<Any>>
			assertTrue(present.size > 1)
			assertTrue(present.toSet().size > 1)
			assertEquals(if (isDescending) present.sortedDescending() else present.sorted(), present)
			assertEquals(keys.filterNotNull().size, keys.indexOfLast { it != null } + 1)
		}
	}

	@Test
	fun `Should page a sort on a repeated key without repeating or skipping elements`() {
		// Arrange
		val sort = SortModel("type")
		val total =
			movementPort.findPage(defaultProjectId, pageable, MovementSearchParamModel(type = null, sort = sort))
				.block()!!
		val pages = (0 until Math.ceilDiv(total.totalElements.toInt(), 7)).map { PageableModel(it * 7L, 7) }

		// Act
		val ids = pages.flatMap {
			movementPort.findPage(defaultProjectId, it, MovementSearchParamModel(type = null, sort = sort))
				.block()!!.content.map { movement -> movement.id }
		}

		// Assert
		assertEquals(total.totalElements.toInt(), ids.size)
		assertEquals(ids.size, ids.toSet().size)
	}

	@Suppress("UNCHECKED_CAST")
	private fun <T : Any> orderCase(
		name: String,
		field: String,
		run: (SortModel) -> Mono<out PageModel<T>>,
		key: (T) -> Comparable<*>?,
	) = OrderCase(name, field, run, { key(it as T) })

	private fun orderCases(): List<OrderCase> = listOf(
		orderCase(
			"alerts",
			"status",
			{
				alertPort.findPage(
					defaultProjectId,
					pageable,
					AlertSearchParamModel(query = null, sort = it)
				)
			}) { it.status?.name },
		orderCase(
			"movements",
			"dateTime",
			{
				movementPort.findPage(
					defaultProjectId,
					pageable,
					MovementSearchParamModel(type = null, sort = it)
				)
			}) { it.dateTime.toInstant() },
		orderCase(
			"participants",
			"birthday",
			{
				participantPort.findPage(
					defaultProjectId,
					pageable,
					ParticipantSearchParamModel(query = null, at = null, sort = it)
				)
			}) { it.birthday },
		orderCase(
			"users",
			"birthday",
			{ userPort.findPage(pageable, UserSearchParamModel(query = null, sort = it)) }) { it.birthday },
		orderCase(
			"activities",
			"minAllowedParticipants",
			{
				activityPort.findPage(
					defaultProjectId,
					pageable,
					ActivitySearchParamModel(query = null, sort = it)
				)
			}) { it.allowedParticipants?.lower },
		orderCase(
			"activities",
			"maxAllowedParticipants",
			{
				activityPort.findPage(
					defaultProjectId,
					pageable,
					ActivitySearchParamModel(query = null, sort = it)
				)
			}) { it.allowedParticipants?.upper },
	)

	private fun directions(case: SortCase, field: String): Stream<DynamicTest> =
		Stream.of(false, true).map { isDescending ->
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
			alertPort.findPage(defaultProjectId, pageable, AlertSearchParamModel(query = null, sort = it))
		},
	)

	private fun communicationCases() = listOf(
		SortCase("movement communications", CommunicationSearchParamModel.SORT_FIELDS) {
			communicationPort.findPageByMovementId(
				defaultProjectId,
				defaultMovementId,
				pageable,
				CommunicationSearchParamModel(query = null, sort = it)
			)
		},
		SortCase("alert communications", CommunicationSearchParamModel.SORT_FIELDS) {
			communicationPort.findPageByAlertId(
				defaultProjectId,
				defaultAlertId,
				pageable,
				CommunicationSearchParamModel(query = null, sort = it)
			)
		},
	)

	private fun groupCases() = listOf(
		SortCase("groups", GroupSearchParamModel.SORT_FIELDS) {
			groupPort.findPage(defaultProjectId, pageable, GroupSearchParamModel(query = null, sort = it))
		},
	)

	private fun movementCases() = listOf(
		SortCase("movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPage(defaultProjectId, pageable, MovementSearchParamModel(type = null, sort = it))
		},
		SortCase("current movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findCurrentPage(defaultProjectId, pageable, MovementSearchParamModel(type = null, sort = it))
		},
		SortCase("participant movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPageByParticipantId(
				defaultProjectId,
				defaultParticipantId,
				pageable,
				MovementSearchParamModel(type = null, sort = it)
			)
		},
		SortCase("vehicle movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPageByVehicleId(
				defaultProjectId,
				defaultVehicleId,
				pageable,
				MovementSearchParamModel(type = null, sort = it)
			)
		},
		SortCase("activity movements", MovementSearchParamModel.SORT_FIELDS) {
			movementPort.findPageByActivityId(
				defaultProjectId,
				defaultActivityId,
				pageable,
				MovementSearchParamModel(type = null, sort = it)
			)
		},
	)

	private fun participantCases() = listOf(
		SortCase("participants", ParticipantSearchParamModel.SORT_FIELDS) {
			participantPort.findPage(
				defaultProjectId,
				pageable,
				ParticipantSearchParamModel(query = null, at = null, sort = it)
			)
		},
		SortCase("group members", ParticipantSearchParamModel.SORT_FIELDS) {
			participantPort.findPageByGroupId(
				defaultProjectId,
				defaultGroupId,
				pageable,
				ParticipantSearchParamModel(query = null, at = null, sort = it)
			)
		},
	)

	private fun projectCases() = listOf(
		SortCase("projects", ProjectSearchParamModel.SORT_FIELDS) {
			projectPort.findPage(pageable, ProjectSearchParamModel(query = null, sort = it))
		},
		SortCase("projects of a list of ids", ProjectSearchParamModel.SORT_FIELDS) {
			projectPort.findPage(listOf(defaultProjectId), pageable, ProjectSearchParamModel(query = null, sort = it))
		},
	)

	private fun profileCases() = listOf(
		SortCase("project profiles", ProjectProfileSearchParamModel.SORT_FIELDS) {
			projectProfilePort.findProjectProfilesPageByProjectId(
				defaultProjectId,
				pageable,
				ProjectProfileSearchParamModel(query = null, sort = it)
			)
		},
		SortCase("user profiles", ProjectProfileSearchParamModel.PROJECT_SORT_FIELDS) {
			projectProfilePort.findProjectProfilesPageByUserId(
				defaultUserId,
				pageable,
				ProjectProfileSearchParamModel(query = null, sort = it)
			)
		},
	)

	private fun userCases() = listOf(
		SortCase("users", UserSearchParamModel.SORT_FIELDS) {
			userPort.findPage(pageable, UserSearchParamModel(query = null, sort = it))
		},
	)

	private fun vehicleCases() = listOf(
		SortCase("vehicles", VehicleSearchParamModel.SORT_FIELDS) {
			vehiclePort.findPage(
				defaultProjectId,
				pageable,
				VehicleSearchParamModel(query = null, at = null, sort = it)
			)
		},
	)
}
