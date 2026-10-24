package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.UserExportModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Purpose: Use-case contract of the personal data export.
 * Scope: Exports the data of a user or of a participant.
 * Limits: Contract only; the implementation owns the authorization.
 */
interface IExportService {
	fun exportUser(currentUser: CurrentUserModel, userId: UUID): Mono<UserExportModel>
	fun exportParticipant(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantExportModel>
}
