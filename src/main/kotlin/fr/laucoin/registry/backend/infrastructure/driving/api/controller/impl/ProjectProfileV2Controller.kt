package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ProjectProfileQueryDto
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PROFILE_RESTORE
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.support.IConcurrencyService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectProfileUseCase
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IProjectProfileV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CreatedProjectProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectProfilesWriterDtoMapper
import java.util.UUID
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST des endpoints du profil de projet.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage du profil de projet.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class ProjectProfileV2Controller(
	private val useCase: IProjectProfileUseCase,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: ProjectProfileReaderDtoMapper,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val projectProfileRoleReaderMapper: ProjectProfileRoleReaderDtoMapper,
	private val createdProjectProfilesReaderMapper: CreatedProjectProfilesReaderDtoMapper,
	private val writerMapper: ProjectProfileWriterDtoMapper,
	private val profilesWriterMapper: ProjectProfilesWriterDtoMapper,
) : IProjectProfileV2Controller {
	override fun findProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		search: ProjectProfileQueryDto,
	): Mono<PageModel<ProjectProfileReaderDto>> {
		val pageable = PageableModel(search.page * search.size, search.size)
		val searchParams = ProjectProfileSearchParamModel(
			search.query, search.isAvailable, search.status, search.at, search.role,
			sort = search.sort.toSortModel(ProjectProfileSearchParamModel.SORT_FIELDS),
			isVisible = currentUser.visibilityFor(search.isVisible, projectId, REGISTRY_PROJECT_PROFILE_RESTORE),
		)

		return useCase.findProjectProfilesPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ProjectProfileReaderDto> {
		val visibility = currentUser.visibilityFor(null, projectId, REGISTRY_PROJECT_PROFILE_RESTORE)

		return useCase.findProjectProfileById(projectId, id, isVisible = visibility).map(readerMapper::toDto)
	}

	override fun searchUsers(projectId: UUID, query: String?): Flux<PartialUserReaderDto> {
		return useCase.searchUsers(query).map(partialUserReaderMapper::toDto)
	}

	override fun getAssignableProjectProfileRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<LabelDto> {
		return useCase.getAssignableProjectRoles(currentUser, projectId).map(projectProfileRoleReaderMapper::toDto)
	}

	override fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: ProjectProfilesWriterDto
	): Mono<ResponseEntity<CreatedProjectProfilesReaderDto>> {
		val profileModels = profilesWriterMapper.toModels(profiles, projectId)

		return useCase.createProjectProfiles(currentUser, projectId, profileModels)
			.map(createdProjectProfilesReaderMapper::toDto)
			.map {
				val status = if (it.notCreatedEmails.isEmpty()) OK else MULTI_STATUS
				ResponseEntity.status(status).body(it)
			}
	}

	override fun updateProjectProfile(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		profile: ProjectProfileWriterDto,
		exchange: ServerWebExchange,
	): Mono<ProjectProfileReaderDto> {
		val profileModel = writerMapper.toModel(profile, projectId)
		return concurrencyService.updateIfUnchanged(
			LockableResourceEnum.PROJECT_PROFILE,
			id,
			exchange.expectedLastEditions(),
			current = { useCase.findProjectProfileById(projectId, id, isVisible = null) },
			action = { useCase.updateProjectProfileById(currentUser, projectId, id, profileModel) },
		).map(readerMapper::toDto)
	}

	override fun blockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileReaderDto> {
		return useCase.blockProjectProfileById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun unblockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileReaderDto> {
		return useCase.unblockProjectProfileById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return useCase.deleteProjectProfileById(currentUser, projectId, id)
	}
}
