package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.UserExportModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage de l'export des données personnelles.
 * Périmètre : Exporte les données d'un utilisateur ou d'un participant.
 * Limites : Contrat uniquement ; l'implémentation porte l'autorisation.
 */
interface IExportService {
	fun exportUser(currentUser: CurrentUserModel, userId: UUID): Mono<UserExportModel>
	fun exportParticipant(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantExportModel>
}
