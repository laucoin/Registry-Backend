package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.ProjectSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IProjectV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectWriterDtoMapper
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ProjectV2Controller(
	private val service: IProjectService,
	private val readerMapper: ProjectReaderDtoMapper,
	private val optionsReaderMapper: ProjectOptionsReaderDtoMapper,
	private val writerMapper: ProjectWriterDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val sortParamMapper: SortParamDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
): IProjectV2Controller {
	override fun findProjects(
		currentUser: CurrentUserModel,
		page: SortedPageQueryDto,
		q: String?,
		visible: Boolean?,
		withProfile: Boolean,
		dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ProjectReaderDto>> {
		if (!currentUser.hasAuthority(REGISTRY_PROJECT_R) && !withProfile) {
			throw RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION)
		}

		val pageable = pageQueryMapper.toPageable(page)
		val sortFields = sortParamMapper.toSortModels(page.sort, page.direction) { key ->
			ProjectSortFieldEnum.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
		}
		val searchParams = ProjectSearchParamModel(q, visible, dateTime)

		return service.findProjectsPage(currentUser, pageable, withProfile, searchParams, sortFields)
			.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun findProjectById(id: UUID): Mono<ProjectReaderDto> {
		return service.findProjectById(id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto> {
		return service.availableProjectOptions().map(optionsReaderMapper::toDto)
	}

	override fun createProject(currentUser: CurrentUserModel, project: ProjectWriterDto): Mono<ResponseEntity<ProjectReaderDto>> {
		val projectModel = writerMapper.toModel(project)
		return service.createProject(currentUser, projectModel).map(readerMapper::toDto).map {
			ResponseEntity.created(URI.create("$API_V2/projects/${it.id}")).body(it)
		}
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

	override fun deleteProjectById(id: UUID): Mono<Unit> = service.deleteProjectById(id)
}
