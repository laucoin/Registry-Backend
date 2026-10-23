package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.UserExportModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IExportService {
	fun exportUser(currentUser: CurrentUserModel, userId: UUID): Mono<UserExportModel>
	fun exportParticipant(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantExportModel>
}
