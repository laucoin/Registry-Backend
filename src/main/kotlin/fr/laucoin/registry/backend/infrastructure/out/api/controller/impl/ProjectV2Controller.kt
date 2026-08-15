package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IProjectV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.OpenAlertProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.OpenAlertProjectReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ProjectWriterDtoMapper
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class ProjectV2Controller(
	private val service: IProjectService,
	private val readerMapper: ProjectReaderDtoMapper,
	private val optionsReaderMapper: ProjectOptionsReaderDtoMapper,
	private val writerMapper: ProjectWriterDtoMapper,
	private val openAlertReaderMapper: OpenAlertProjectReaderDtoMapper,
) : IProjectV2Controller {

	override fun findOpenAlertProjects(
		currentUser: CurrentUserModel,
		limit: Int,
	): Flux<OpenAlertProjectReaderDto> {
		return service.findOpenAlertProjects(currentUser, limit).map(openAlertReaderMapper::toDto)
	}

	override fun findProjects(
		currentUser: CurrentUserModel,
		page: Int,
		size: Int,
		sort: List<String>?,
		direction: String,
		q: String?,
		visible: Boolean?,
		withProfile: Boolean,
		dateTime: ZonedDateTime?,
	): Mono<PageModel<ProjectReaderDto>> {
		if (!currentUser.hasAuthority(REGISTRY_PROJECT_R) && !withProfile) {
			throw RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
		}

		val pageable = PageableModel(page * size, size)
		val searchParams = ProjectSearchParamModel(q, visible, dateTime)
		val sortModels = SortParamDtoMapper.toSortModels(sort, direction, ProjectSortFieldEnum::fromParamName)

		return service.findProjectsPage(currentUser, pageable, withProfile, searchParams, sortModels)
			.map(readerMapper::toDtoPage)
	}

	override fun findProjectById(id: UUID): Mono<ProjectReaderDto> {
		return service.findProjectById(id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto> {
		return service.availableProjectOptions().map(optionsReaderMapper::toDto)
	}

	override fun createProject(currentUser: CurrentUserModel, project: ProjectWriterDto): Mono<ProjectReaderDto> {
		val projectModel = writerMapper.toModel(project)
		return service.createProject(currentUser, projectModel).map(readerMapper::toDto)
	}

	override fun updateProjectById(
		currentUser: CurrentUserModel,
		id: UUID,
		project: ProjectWriterDto,
	): Mono<ProjectReaderDto> {
		val projectModel = writerMapper.toModel(project)
		return service.updateProjectById(currentUser, id, projectModel).map(readerMapper::toDto)
	}

	override fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectReaderDto> {
		return service.disableProjectById(currentUser, id).map(readerMapper::toDto)
	}

	override fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectReaderDto> {
		return service.enableProjectById(currentUser, id).map(readerMapper::toDto)
	}

	override fun deleteProjectById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> =
		service.deleteProjectById(currentUser, id)
}
