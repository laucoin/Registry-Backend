package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IProjectProfileController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ProjectProfilesWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.CreatedProjectProfilesReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectProfileRoleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ProjectProfileWriterDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.ProjectProfilesWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ProjectProfileController(
    private val service: IProjectProfileService,
    private val readerMapper: ProjectProfileReaderDtoMapper,
    private val partialUserReaderMapper: PartialUserReaderDtoMapper,
    private val projectProfileRoleReaderMapper: ProjectProfileRoleReaderDtoMapper,
    private val createdProjectProfilesReaderMapper: CreatedProjectProfilesReaderDtoMapper,
    private val writerMapper: ProjectProfileWriterDtoMapper,
    private val profilesWriterMapper: ProjectProfilesWriterDtoMapper,
): IProjectProfileController {
    override fun findProjectProfiles(
        locale: Locale,
        projectId: UUID,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        availabilitySearched: Boolean?,
        statusSearched: ProfileStatusEnum?,
        dateTimeSearched: ZonedDateTime?
    ): Mono<PageModel<ProjectProfileReaderDto>> {
        return service.findProjectProfilesPage(
            projectId,
            PageableModel(pageNumber * pageSize, pageSize),
            ProjectProfileSearchParamModel(
                textSearched,
                availabilitySearched,
                statusSearched,
                dateTimeSearched
            ),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findProjectProfileById(locale: Locale, projectId: UUID, id: UUID): Mono<ProjectProfileReaderDto> {
        return service.findProjectProfileById(projectId, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun searchUsers(locale: Locale, projectId: UUID, textSearched: String?): Flux<PartialUserReaderDto> {
        return service.searchUsers(textSearched)
            .map { partialUserReaderMapper.toDto(it, locale) }
    }

    override fun getAssignableProjectProfileRoles(currentUser: CurrentUserModel, locale: Locale, projectId: UUID): Flux<LabelDto> {
        return service.getAssignableProjectRoles(currentUser, projectId)
            .map { projectProfileRoleReaderMapper.toDto(it, locale) }
    }

    override fun createProjectProfiles(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        profiles: ProjectProfilesWriterDto
    ): Mono<ResponseEntity<CreatedProjectProfilesReaderDto>> {
        return service.createProjectProfiles(
            currentUser,
            projectId,
            profiles.userIds !!,
            profilesWriterMapper.toModels(profiles, projectId)
        )
            .map { createdProjectProfilesReaderMapper.toDto(it, locale) }
            .map { ResponseEntity.status(if (it.notCreatedUserIds.isEmpty()) OK else MULTI_STATUS).body(it) }
    }

    override fun updateProjectProfile(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
        profile: ProjectProfileWriterDto,
    ): Mono<ProjectProfileReaderDto> {
        return service.updateProjectProfileById(currentUser, projectId, id, writerMapper.toModel(profile, projectId))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun blockProjectProfileById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
    ): Mono<ProjectProfileReaderDto> {
        return service.blockProjectProfileById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun unblockProjectProfileById(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID,
        id: UUID,
    ): Mono<ProjectProfileReaderDto> {
        return service.unblockProjectProfileById(currentUser, projectId, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
        return service.deleteProjectProfileById(currentUser, projectId, id)
    }
}
