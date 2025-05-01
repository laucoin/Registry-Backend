package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IProjectController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ProjectWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ProjectController(
    private val service: IProjectService,
    private val readerMapper: ProjectReaderDtoMapper,
    private val optionsReaderMapper: ProjectOptionsReaderDtoMapper,
    private val writerMapper: ProjectWriterDtoMapper,
): IProjectController {
    override fun findProjects(
        currentUser: CurrentUserModel,
        locale: Locale,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        withProfile: Boolean,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<ProjectReaderDto>> {
        return service.findProjectsPage(
            currentUser,
            PageableModel(pageNumber * pageSize, pageSize),
            withProfile,
            ProjectSearchParamModel(textSearched, visibilitySearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findProjectById(locale: Locale, id: UUID): Mono<ProjectReaderDto> {
        return service.findProjectById(id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun getAvailableProjectOptions(locale: Locale): Flux<ProjectOptionsReaderDto> {
        return service.availableProjectOptions()
            .map { optionsReaderMapper.toDto(it, locale) }
    }

    override fun createProject(currentUser: CurrentUserModel, locale: Locale, project: ProjectWriterDto): Mono<ProjectReaderDto> {
        return service.createProject(currentUser, writerMapper.toModel(project))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateProjectById(
        currentUser: CurrentUserModel,
        locale: Locale,
        id: UUID,
        project: ProjectWriterDto
    ): Mono<ProjectReaderDto> {
        return service.updateProjectById(currentUser, id, writerMapper.toModel(project))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableProjectById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<ProjectReaderDto> {
        return service.disableProjectById(currentUser, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableProjectById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<ProjectReaderDto> {
        return service.enableProjectById(currentUser, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteProjectById(id: UUID): Mono<Void> = service.deleteProjectById(id)
}
