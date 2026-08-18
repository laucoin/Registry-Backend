package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IProjectProfileV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.CreatedProjectProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ProjectProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ProjectProfilesWriterDtoMapper
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class ProjectProfileV2Controller(
	private val service: IProjectProfileService,
	private val readerMapper: ProjectProfileReaderDtoMapper,
	private val partialUserReaderMapper: PartialUserReaderDtoMapper,
	private val projectProfileRoleReaderMapper: ProjectProfileRoleReaderDtoMapper,
	private val createdProjectProfilesReaderMapper: CreatedProjectProfilesReaderDtoMapper,
	private val writerMapper: ProjectProfileWriterDtoMapper,
	private val profilesWriterMapper: ProjectProfilesWriterDtoMapper,
) : IProjectProfileV2Controller {
	override fun findProjectProfiles(
		projectId: UUID,
		pageQuery: PageQueryDto,
		q: String?,
		available: Boolean?,
		status: ProfileStatusEnum?,
		dateTime: ZonedDateTime?,
	): Mono<PageModel<ProjectProfileReaderDto>> {
		val pageable = PageQueryDtoMapper.toPageable(pageQuery)
		val searchParams = ProjectProfileSearchParamModel(q, available, status, dateTime)

		return service.findProjectProfilesPage(projectId, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findProjectProfileById(projectId: UUID, id: UUID): Mono<ProjectProfileReaderDto> {
		return service.findProjectProfileById(projectId, id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun searchAssignableUsers(projectId: UUID, q: String?): Flux<PartialUserReaderDto> {
		return service.searchUsers(q).map(partialUserReaderMapper::toDto)
	}

	override fun getAssignableProjectProfileRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<LabelDto> {
		return service.getAssignableProjectRoles(currentUser, projectId).map(projectProfileRoleReaderMapper::toDto)
	}

	override fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: ProjectProfilesWriterDto
	): Mono<ResponseEntity<CreatedProjectProfilesReaderDto>> {
		val template = profilesWriterMapper.toTemplate(profiles, projectId)

		return service.createProjectProfiles(
			currentUser,
			projectId,
			profiles.userIds ?: emptyList(),
			profiles.emails ?: emptyList(),
			template,
		)
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
	): Mono<ProjectProfileReaderDto> {
		val profileModel = writerMapper.toModel(profile, projectId)
		return service.updateProjectProfileById(currentUser, projectId, id, profileModel).map(readerMapper::toDto)
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
