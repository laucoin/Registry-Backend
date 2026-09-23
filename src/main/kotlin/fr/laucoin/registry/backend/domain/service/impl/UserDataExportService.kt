package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantDataExportModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserDataExportModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IUserDataExportService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserDataExportService(
	private val userPort: IUserPort,
	private val preferencesPort: IPreferencesPort,
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
		val preferences = preferencesPort.findByUserId(userId, visibilitySearched = null)
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

		return Mono.zip(
			listOf(
				user,
				preferences,
				projectProfiles,
				createdProjectProfiles,
				movements,
				communications,
				alerts,
				activities,
				vehicles,
				groups,
				linkedParticipants,
			)
		) { results ->
			@Suppress("UNCHECKED_CAST")
			UserDataExportModel(
				user = results[0] as UserModel,
				preferences = results[1] as PreferencesModel,
				projectProfiles = results[2] as List<ProjectProfileModel>,
				createdProjectProfiles = results[3] as List<ProjectProfileModel>,
				movements = results[4] as List<MovementModel>,
				communications = results[5] as List<CommunicationModel>,
				alerts = results[6] as List<AlertModel>,
				activities = results[7] as List<ActivityModel>,
				vehicles = results[8] as List<VehicleModel>,
				groups = results[9] as List<GroupModel>,
				linkedParticipants = results[10] as List<ParticipantDataExportModel>,
			)
		}
	}
}
