package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IUserProjectProfileV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class UserProjectProfileController(
	private val service: IUserProjectProfileService,
	private val readerMapper: ProjectProfileReaderDtoMapper,
) : IUserProjectProfileV1Controller {
	override fun findUserProjectProfiles(
		currentUser: CurrentUserModel,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		availabilitySearched: Boolean?,
		statusSearched: ProfileStatusEnum?,
		dateTimeSearched: ZonedDateTime?,
	): Mono<PageModel<ProjectProfileReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = ProjectProfileSearchParamModel(
			textSearched, availabilitySearched, statusSearched, dateTimeSearched
		)

		return service.findProjectProfilesPage(currentUser.id!!, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun manageUserProjectProfileAcceptance(
		currentUser: CurrentUserModel,
		id: UUID,
		accepted: Boolean,
	): Mono<ProjectProfileReaderDto> {
		val status = if (accepted) ProfileStatusEnum.ACCEPTED else ProfileStatusEnum.REJECTED
		return service.updateUserProjectProfileStatusById(currentUser, id, status).map(readerMapper::toDto)
	}

	override fun createSupportProjectProfile(
		currentUser: CurrentUserModel,
		projectId: UUID
	): Mono<ProjectProfileReaderDto> {
		return service.createSupportProjectProfile(currentUser, projectId).map(readerMapper::toDto)
	}

	override fun deleteUserProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserProjectProfileById(currentUser, id)
	}
}
