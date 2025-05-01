package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IUserProjectProfileController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectProfileReaderDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserProjectProfileController(
    private val service: IUserProjectProfileService,
    private val readerMapper: ProjectProfileReaderDtoMapper,
): IUserProjectProfileController {
    override fun findUserProjectProfiles(
        currentUser: CurrentUserModel,
        locale: Locale,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        availabilitySearched: Boolean?,
        statusSearched: ProfileStatusEnum?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<ProjectProfileReaderDto>> {
        return service.findProjectProfilesPage(
            currentUser.id !!,
            PageableModel(pageNumber * pageSize, pageSize),
            ProjectProfileSearchParamModel(textSearched, availabilitySearched, statusSearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findUserProjectProfileById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<ProjectProfileReaderDto> {
        return service.findUserProjectProfileById(currentUser, id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun manageUserProjectProfileAcceptance(
        currentUser: CurrentUserModel,
        locale: Locale,
        id: UUID,
        accepted: Boolean,
    ): Mono<ProjectProfileReaderDto> {
        return service.updateUserProjectProfileStatusById(
            currentUser,
            id,
            if (accepted) ProfileStatusEnum.ACCEPTED else ProfileStatusEnum.REJECTED
        ).map { readerMapper.toDto(it, locale) }
    }

    override fun createSupportProjectProfile(
        currentUser: CurrentUserModel,
        locale: Locale,
        projectId: UUID
    ): Mono<ProjectProfileReaderDto> {
        return service.createSupportProjectProfile(currentUser, projectId)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteUserProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Void> {
        return service.deleteUserProjectProfileById(currentUser, id)
    }
}
