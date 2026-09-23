package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserDataExportModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IUserDataExportService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserDataExportService(
	private val userPort: IUserPort,
	private val preferencesService: IPreferencesService,
	private val projectProfilePort: IProjectProfilePort,
	private val movementPort: IMovementPort,
	private val communicationPort: ICommunicationPort,
	private val alertPort: IAlertPort,
	private val activityPort: IActivityPort,
	private val vehiclePort: IVehiclePort,
	private val groupPort: IGroupPort,
	private val participantPort: IParticipantPort,
	private val participantService: IParticipantService,
): IUserDataExportService {
	private companion object {
		private const val EXPORT_MAX_ROWS = 10_000
	}

	override fun exportCurrentUserData(currentUser: CurrentUserModel): Mono<UserDataExportModel> {
		val userId = currentUser.id!!
		val pageable = PageableModel(0, EXPORT_MAX_ROWS)

		val user = userPort.findById(userId, visibilitySearched = null).notFoundIfEmpty(userId)
		val preferences = preferencesService.findByUser(currentUser)
		val projectProfiles = projectProfilePort
			.findProjectProfilesPageByUserId(
				userId,
				pageable,
				ProjectProfileSearchParamModel(visibilitySearched = null),
				emptyList(),
			)
			.map { it.content }
		val createdProjectProfiles = projectProfilePort.findAllByCreatorId(userId).collectList()
		val movements = movementPort.findAllByCreatorId(userId).collectList()
		val communications = communicationPort.findAllByCreatorId(userId).collectList()
		val alerts = alertPort.findAllByCreatorId(userId).collectList()
		val activities = activityPort.findAllByCreatorId(userId).collectList()
		val vehicles = vehiclePort.findAllByCreatorId(userId).collectList()
		val groups = groupPort.findAllByCreatorId(userId).collectList()
		val linkedParticipants = participantPort.findAllByUserId(userId)
			.flatMap { participantService.exportParticipantData(it.project!!.id!!, it.id!!) }
			.collectList()

		val accountAndContent = Mono.zip(user, preferences, projectProfiles, createdProjectProfiles, movements, communications, alerts, activities)
		val remainingContent = Mono.zip(vehicles, groups, linkedParticipants)

		return Mono.zip(accountAndContent, remainingContent) { first, second ->
			UserDataExportModel(
				user = first.t1,
				preferences = first.t2,
				projectProfiles = first.t3,
				createdProjectProfiles = first.t4,
				movements = first.t5,
				communications = first.t6,
				alerts = first.t7,
				activities = first.t8,
				vehicles = second.t1,
				groups = second.t2,
				linkedParticipants = second.t3,
			)
		}
	}
}
