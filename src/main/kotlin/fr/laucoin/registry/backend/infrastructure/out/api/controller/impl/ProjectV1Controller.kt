package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IProjectV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ProjectWriterDto
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
class ProjectV1Controller(
	private val service: IProjectService,
	private val readerMapper: ProjectReaderDtoMapper,
	private val optionsReaderMapper: ProjectOptionsReaderDtoMapper,
	private val writerMapper: ProjectWriterDtoMapper,
) : IProjectV1Controller {
	override fun findProjects(
		currentUser: CurrentUserModel,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		withProfile: Boolean,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ProjectReaderDto>> {
		if (!currentUser.hasAuthority(REGISTRY_PROJECT_R) && !withProfile) {
			throw RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
		}

		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = ProjectSearchParamModel(textSearched, visibilitySearched, dateTimeSearched)

		return service.findProjectsPage(currentUser, pageable, withProfile, searchParams).map(readerMapper::toDtoPage)
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
