package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IProjectV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectWithProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ProjectWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class ProjectV2Controller(
	private val service: IProjectService,
	private val readerMapper: ProjectWithProfileReaderDtoMapper,
	private val writerMapper: ProjectWriterDtoMapper,
): IProjectV2Controller {
	override fun findProjects(
		currentUser: CurrentUserModel,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ProjectWithProfileReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = ProjectSearchParamModel(textSearched, visibilitySearched, dateTimeSearched)

		// Without the global read authority, only the Projects the User has a valid Profile on are visible
		val withProfile = !currentUser.hasAuthority(REGISTRY_PROJECT_R)

		return service.findProjectsPage(currentUser, pageable, withProfile, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectWithProfileReaderDto> {
		return service.findProjectWithProfileById(currentUser, id).map(readerMapper::toDto)
	}

	override fun createProject(currentUser: CurrentUserModel, project: ProjectWriterDto): Mono<ProjectWithProfileReaderDto> {
		val projectModel = writerMapper.toModel(project)
		return service.createProject(currentUser, projectModel).map(readerMapper::toDto)
	}

	override fun updateProjectById(
		currentUser: CurrentUserModel,
		id: UUID,
		project: ProjectWriterDto,
	): Mono<ProjectWithProfileReaderDto> {
		val projectModel = writerMapper.toModel(project)
		return service.updateProjectById(currentUser, id, projectModel).map(readerMapper::toDto)
	}

	override fun disableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectWithProfileReaderDto> {
		return service.disableProjectById(currentUser, id).map(readerMapper::toDto)
	}

	override fun enableProjectById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectWithProfileReaderDto> {
		return service.enableProjectById(currentUser, id).map(readerMapper::toDto)
	}

	override fun deleteProjectById(id: UUID): Mono<Unit> = service.deleteProjectById(id)
}
