package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.service.IConcurrencyService
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
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
import java.time.ZonedDateTime
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
	private val service: IProjectProfileService,
	private val concurrencyService: IConcurrencyService,
	private val readerMapper: ProjectProfileReaderDtoMapper,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val projectProfileRoleReaderMapper: ProjectProfileRoleReaderDtoMapper,
	private val createdProjectProfilesReaderMapper: CreatedProjectProfilesReaderDtoMapper,
	private val writerMapper: ProjectProfileWriterDtoMapper,
	private val profilesWriterMapper: ProjectProfilesWriterDtoMapper,
) : IProjectProfileV2Controller {
	override fun findProjectProfiles(
		projectId: UUID,
		page: Int,
		size: Int,
		query: String?,
		isAvailable: Boolean?,
		status: ProfileStatusEnum?,
		at: ZonedDateTime?,
		role: String?,
		sort: String?,
	): Mono<PageModel<ProjectProfileReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = ProjectProfileSearchParamModel(
			query, isAvailable, status, at, role,
			sort = sort.toSortModel(ProjectProfileSearchParamModel.SORT_FIELDS),
		)

		return service.findProjectProfilesPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findProjectProfileById(projectId: UUID, id: UUID): Mono<ProjectProfileReaderDto> {
		return service.findProjectProfileById(projectId, id, isVisible = null).map(readerMapper::toDto)
	}

	override fun searchUsers(projectId: UUID, query: String?): Flux<PartialUserReaderDto> {
		return service.searchUsers(query).map(partialUserReaderMapper::toDto)
	}

	override fun getAssignableProjectProfileRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<LabelDto> {
		return service.getAssignableProjectRoles(currentUser, projectId).map(projectProfileRoleReaderMapper::toDto)
	}

	override fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: ProjectProfilesWriterDto
	): Mono<ResponseEntity<CreatedProjectProfilesReaderDto>> {
		val profileModels = profilesWriterMapper.toModels(profiles, projectId)

		return service.createProjectProfiles(currentUser, projectId, profiles.userIds!!, profileModels)
			.map(createdProjectProfilesReaderMapper::toDto)
			.map {
				val status = if (it.notCreatedUserIds.isEmpty()) OK else MULTI_STATUS
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
			current = { service.findProjectProfileById(projectId, id, isVisible = null) },
			action = { service.updateProjectProfileById(currentUser, projectId, id, profileModel) },
		).map(readerMapper::toDto)
	}

	override fun blockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileReaderDto> {
		return service.blockProjectProfileById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun unblockProjectProfileById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
	): Mono<ProjectProfileReaderDto> {
		return service.unblockProjectProfileById(currentUser, projectId, id).map(readerMapper::toDto)
	}

	override fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return service.deleteProjectProfileById(currentUser, projectId, id)
	}
}
