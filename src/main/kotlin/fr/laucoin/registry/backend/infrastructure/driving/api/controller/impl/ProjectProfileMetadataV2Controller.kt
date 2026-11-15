package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.usecase.IProjectProfileUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IProjectProfileMetadataV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileRoleReaderDtoMapper
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

/**
 * Objectif : Adaptateur REST des métadonnées du profil de projet (recherche d'utilisateurs, rôles assignables).
 * Périmètre : Mappe les modèles en DTO et délègue au cas d'usage des profils de projet.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class ProjectProfileMetadataV2Controller(
	private val useCase: IProjectProfileUseCase,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val projectProfileRoleReaderMapper: ProjectProfileRoleReaderDtoMapper,
): IProjectProfileMetadataV2Controller {
	override fun searchUsers(projectId: UUID, query: String?): Flux<PartialUserReaderDto> {
		return useCase.searchUsers(query).map(partialUserReaderMapper::toDto)
	}

	override fun getAssignableProjectProfileRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<LabelDto> {
		return useCase.getAssignableProjectRoles(currentUser, projectId).map(projectProfileRoleReaderMapper::toDto)
	}
}
