package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IUserProjectProfileV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectWithProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserInvitationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectWithProfileReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.UserInvitationReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class UserProjectProfileV2Controller(
	private val service: IUserProjectProfileService,
	private val projectService: IProjectService,
	private val readerMapper: UserInvitationReaderDtoMapper,
	private val projectReaderMapper: ProjectWithProfileReaderDtoMapper,
) : IUserProjectProfileV2Controller {
	override fun findUserProjectProfiles(
		currentUser: CurrentUserModel,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		availabilitySearched: Boolean?,
		statusSearched: ProfileStatusEnum?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<UserInvitationReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = ProjectProfileSearchParamModel(
			textSearched, availabilitySearched, statusSearched, dateTimeSearched
		)

		return service.findProjectProfilesPage(currentUser.id!!, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findUserProjectProfileById(
		currentUser: CurrentUserModel,
		id: UUID
	): Mono<UserInvitationReaderDto> {
		return service.findProjectProfilesById(currentUser, id).map(readerMapper::toDto)
	}

	override fun manageUserProjectProfileAcceptance(
		currentUser: CurrentUserModel,
		id: UUID,
		accepted: Boolean,
	): Mono<UserInvitationReaderDto> {
		val status = if (accepted) ProfileStatusEnum.ACCEPTED else ProfileStatusEnum.REJECTED
		return service.updateUserProjectProfileStatusById(currentUser, id, status).map(readerMapper::toDto)
	}

	override fun createSupportProjectProfile(
		currentUser: CurrentUserModel,
		projectId: UUID
	): Mono<ProjectWithProfileReaderDto> {
		return service.createSupportProjectProfile(currentUser, projectId)
			.then(projectService.findProjectWithProfileById(currentUser, projectId))
			.map(projectReaderMapper::toDto)
	}

	override fun deleteUserProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserProjectProfileById(currentUser, id)
	}
}
