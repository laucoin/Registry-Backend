package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_NOT_SUPPORTED
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ACTIVITY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_ALERT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_COMMUNICATION_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_VEHICLE_R
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_USER_R
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.alertId
import fr.laucoin.registry.backend.test.ModelExt.groupId
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.participantId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.ModelExt.vehicleId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.stream.Stream
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.test.web.reactive.server.WebTestClient

class ListSortingContractTest : TestContext() {
	private data class ListEndpoint(
		val name: String,
		val uri: String,
		val params: List<Any>,
		val authorities: List<String>,
		val sortFields: Set<String>,
		val hasQuery: Boolean = true,
	)

	@Autowired
	private lateinit var webClient: WebTestClient

	private val project = "/api/v2/projects/{projectId}"

	@TestFactory
	fun `Should refuse a sort field outside the allow-list of every list`(): Stream<DynamicTest> {
		return endpoints().stream().map { endpoint ->
			dynamicTest(endpoint.name) {
				// Arrange
				val uri = uriBuilder(endpoint.uri, endpoint.params, listOf(Pair("sort", "-notASortField")))

				// Act
				val result = webClient
					.authenticate(*endpoint.authorities.toTypedArray())
					.get()
					.uri(uri)
					.exchange()

				// Assert
				result.assertError(BAD_REQUEST, SORT_FIELD_NOT_SUPPORTED)
			}
		}
	}

	@TestFactory
	fun `Should accept every supported sort field on every list`(): Stream<DynamicTest> {
		return endpoints().stream().flatMap { endpoint ->
			endpoint.sortFields.stream().map { field ->
				dynamicTest("${endpoint.name} sorted by -$field") {
					// Arrange
					val uri = uriBuilder(endpoint.uri, endpoint.params, listOf(Pair("sort", "-$field")))

					// Act
					val result = webClient
						.authenticate(*endpoint.authorities.toTypedArray())
						.get()
						.uri(uri)
						.exchange()

					// Assert
					result.expectStatus().isOk
				}
			}
		}
	}

	@TestFactory
	fun `Should refuse a sort and a text search over their length cap on every list`(): Stream<DynamicTest> {
		return endpoints().stream().flatMap { endpoint ->
			Stream.of(Pair("sort", 33), Pair("q", 101)).filter { (name, _) -> name == "sort" || endpoint.hasQuery }.map { (name, length) ->
				dynamicTest("${endpoint.name} with a $name of $length characters") {
					// Arrange
					val uri = uriBuilder(endpoint.uri, endpoint.params, listOf(Pair(name, "a".repeat(length))))

					// Act
					val result = webClient
						.authenticate(*endpoint.authorities.toTypedArray())
						.get()
						.uri(uri)
						.exchange()

					// Assert
					result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
				}
			}
		}
	}

	@TestFactory
	fun `Should refuse a text search over its length cap on every picker`(): Stream<DynamicTest> {
		return pickers().stream().map { picker ->
			dynamicTest(picker.name) {
				// Arrange
				val uri = uriBuilder(picker.uri, picker.params, picker.fixedQuery + Pair("q", "a".repeat(101)))

				// Act
				val result = webClient
					.authenticate(*picker.authorities.toTypedArray())
					.get()
					.uri(uri)
					.exchange()

				// Assert
				result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
			}
		}
	}

	@TestFactory
	fun `Should accept a text search at exactly its length cap on every picker`(): Stream<DynamicTest> {
		return pickers().stream().map { picker ->
			dynamicTest(picker.name) {
				// Arrange
				val uri = uriBuilder(picker.uri, picker.params, picker.fixedQuery + Pair("q", "a".repeat(100)))

				// Act
				val result = webClient
					.authenticate(*picker.authorities.toTypedArray())
					.get()
					.uri(uri)
					.exchange()

				// Assert
				result.expectStatus().isOk
			}
		}
	}

	@TestFactory
	fun `Should accept a sort and a text search at exactly their length cap on every list`(): Stream<DynamicTest> {
		return endpoints().stream().flatMap { endpoint ->
			Stream.of(Pair("sort", 32), Pair("q", 100)).filter { (name, _) -> name == "sort" || endpoint.hasQuery }.map { (name, length) ->
				dynamicTest("${endpoint.name} with a $name of $length characters") {
					// Arrange
					val uri = uriBuilder(endpoint.uri, endpoint.params, listOf(Pair(name, "a".repeat(length))))

					// Act
					val result = webClient
						.authenticate(*endpoint.authorities.toTypedArray())
						.get()
						.uri(uri)
						.exchange()

					// Assert
					if (name == "sort") result.assertError(BAD_REQUEST, SORT_FIELD_NOT_SUPPORTED) else result.expectStatus().isOk
				}
			}
		}
	}

	private data class Picker(
		val name: String,
		val uri: String,
		val params: List<Any>,
		val authorities: List<String>,
		val fixedQuery: List<Pair<String, Any?>> = emptyList(),
	)

	private fun pickers(): List<Picker> {
		val inProject = listOf<Any>(projectId)
		return listOf(
			Picker("group participants", "$project/groups/search/participants", inProject, listOf(perm(REGISTRY_PROJECT_GROUP_METADATA_R))),
			Picker("participant users", "$project/participants/search/users", inProject, listOf(perm(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))),
			Picker("participant groups", "$project/participants/search/groups", inProject, listOf(perm(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))),
			Picker("profile users", "$project/profiles/search/users", inProject, listOf(perm(REGISTRY_PROJECT_PROFILE_METADATA_R))),
			Picker("communication movements", "$project/communications/search/movements", inProject, listOf(option(REGISTRY_PROJECT_OPTION_COMMUNICATION), perm(REGISTRY_PROJECT_COMMUNICATION_METADATA_R))),
			Picker("communication alerts", "$project/communications/search/alerts", inProject, listOf(option(REGISTRY_PROJECT_OPTION_ALERT), perm(REGISTRY_PROJECT_COMMUNICATION_METADATA_R))),
			Picker("movement reasons", "$project/movements/search/reasons", inProject, listOf(perm(REGISTRY_PROJECT_MOVEMENT_METADATA_R)), listOf(Pair("type", "IN"), Pair("contentType", "REGISTERED"))),
			Picker("movement participants and groups", "$project/movements/search/participants-and-groups", inProject, listOf(perm(REGISTRY_PROJECT_MOVEMENT_METADATA_R)), listOf(Pair("contentType", "REGISTERED"))),
			Picker("movement vehicles", "$project/movements/search/vehicles", inProject, listOf(perm(REGISTRY_PROJECT_MOVEMENT_METADATA_R))),
		)
	}

	private fun endpoints(): List<ListEndpoint> = projectEndpoints() + movementEndpoints() + globalEndpoints()

	private fun projectEndpoints(): List<ListEndpoint> {
		val inProject = listOf<Any>(projectId)
		return listOf(
			ListEndpoint("activities", "$project/activities", inProject, listOf(option(REGISTRY_PROJECT_OPTION_ACTIVITY), perm(REGISTRY_PROJECT_ACTIVITY_R)), ActivitySearchParamModel.SORT_FIELDS),
			ListEndpoint("alerts", "$project/alerts", inProject, listOf(option(REGISTRY_PROJECT_OPTION_ALERT), perm(REGISTRY_PROJECT_ALERT_R)), AlertSearchParamModel.SORT_FIELDS),
			ListEndpoint("alert communications", "$project/alerts/{id}/communications", listOf(projectId, alertId), listOf(option(REGISTRY_PROJECT_OPTION_ALERT), perm(REGISTRY_PROJECT_ALERT_COMMUNICATION_R)), CommunicationSearchParamModel.SORT_FIELDS),
			ListEndpoint("groups", "$project/groups", inProject, listOf(perm(REGISTRY_PROJECT_GROUP_R)), GroupSearchParamModel.SORT_FIELDS),
			ListEndpoint("group members", "$project/groups/{id}/members", listOf(projectId, groupId), listOf(perm(REGISTRY_PROJECT_GROUP_R)), ParticipantSearchParamModel.SORT_FIELDS),
			ListEndpoint("participants", "$project/participants", inProject, listOf(perm(REGISTRY_PROJECT_PARTICIPANT_R)), ParticipantSearchParamModel.SORT_FIELDS),
			ListEndpoint("project profiles", "$project/profiles", inProject, listOf(perm(REGISTRY_PROJECT_PROFILE_R)), ProjectProfileSearchParamModel.SORT_FIELDS),
			ListEndpoint("vehicles", "$project/vehicles", inProject, listOf(option(REGISTRY_PROJECT_OPTION_VEHICLE), perm(REGISTRY_PROJECT_VEHICLE_R)), VehicleSearchParamModel.SORT_FIELDS),
		)
	}

	private fun movementEndpoints(): List<ListEndpoint> = listOf(
		ListEndpoint("movements", "$project/movements", listOf(projectId), listOf(perm(REGISTRY_PROJECT_MOVEMENT_R)), MovementSearchParamModel.SORT_FIELDS, hasQuery = false),
		ListEndpoint("movement communications", "$project/movements/{id}/communications", listOf(projectId, movementId), listOf(option(REGISTRY_PROJECT_OPTION_COMMUNICATION), perm(REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R)), CommunicationSearchParamModel.SORT_FIELDS),
		ListEndpoint("activity movements", "$project/activities/{id}/movements", listOf(projectId, activityId), listOf(option(REGISTRY_PROJECT_OPTION_ACTIVITY), perm(REGISTRY_PROJECT_ACTIVITY_HISTORY_R)), MovementSearchParamModel.SORT_FIELDS, hasQuery = false),
		ListEndpoint("participant movements", "$project/participants/{id}/movements", listOf(projectId, participantId), listOf(perm(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R)), MovementSearchParamModel.SORT_FIELDS, hasQuery = false),
		ListEndpoint("vehicle movements", "$project/vehicles/{id}/movements", listOf(projectId, vehicleId), listOf(option(REGISTRY_PROJECT_OPTION_VEHICLE), perm(REGISTRY_PROJECT_VEHICLE_HISTORY_R)), MovementSearchParamModel.SORT_FIELDS, hasQuery = false),
	)

	private fun globalEndpoints(): List<ListEndpoint> = listOf(
		ListEndpoint("users", "/api/v2/users", emptyList(), listOf(REGISTRY_USER_R), UserSearchParamModel.SORT_FIELDS),
		ListEndpoint("projects", "/api/v2/projects", emptyList(), emptyList(), ProjectSearchParamModel.SORT_FIELDS),
		ListEndpoint("user profiles", "/api/v2/users/profiles", emptyList(), emptyList(), ProjectProfileSearchParamModel.PROJECT_SORT_FIELDS),
	)

	private fun perm(authority: String) = buildAuthority(authority)

	private fun option(authority: String) = buildAuthority(authority)
}
