package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IUserProjectProfileV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.PageQueryDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileReaderDtoMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@RestController
class UserProjectProfileV2Controller(
	private val service: IUserProjectProfileService,
	private val readerMapper: ProjectProfileReaderDtoMapper,
	@param:Value($$"${registry.feature.dashboard.sent-invitation-window-days:2}")
	private val sentInvitationWindowDays: Long,
) : IUserProjectProfileV2Controller {
	override fun findUserProjectProfiles(
		currentUser: CurrentUserModel,
		pageQuery: PageQueryDto,
		q: String?,
		available: Boolean?,
		status: ProfileStatusEnum?,
		dateTime: ZonedDateTime?,
		favorite: Boolean?,
	): Mono<PageModel<ProjectProfileReaderDto>> {
		val pageable = PageQueryDtoMapper.toPageable(pageQuery)
		val searchParams = ProjectProfileSearchParamModel(q, available, status, dateTime)
			.apply { favoriteSearched = favorite }

		return service.findProjectProfilesPage(currentUser.id!!, pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun toggleFavoriteUserProjectProfileById(
		currentUser: CurrentUserModel,
		id: UUID,
	): Mono<ProjectProfileReaderDto> {
		return service.toggleFavorite(currentUser, id).map(readerMapper::toDto)
	}

	override fun findSentInvitations(
		currentUser: CurrentUserModel,
		pageQuery: PageQueryDto,
	): Mono<PageModel<ProjectProfileReaderDto>> {
		val pageable = PageQueryDtoMapper.toPageable(pageQuery)
		val since = ZonedDateTime.now().minusDays(sentInvitationWindowDays)
		return service.findSentInvitationsPage(currentUser, pageable, since).map(readerMapper::toDtoPage)
	}

	override fun acceptUserProjectProfileById(
		currentUser: CurrentUserModel,
		id: UUID,
	): Mono<ProjectProfileReaderDto> {
		return service.updateUserProjectProfileStatusById(currentUser, id, ProfileStatusEnum.ACCEPTED)
			.map(readerMapper::toDto)
	}

	override fun rejectUserProjectProfileById(
		currentUser: CurrentUserModel,
		id: UUID,
	): Mono<ProjectProfileReaderDto> {
		return service.updateUserProjectProfileStatusById(currentUser, id, ProfileStatusEnum.REJECTED)
			.map(readerMapper::toDto)
	}


	override fun deleteUserProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserProjectProfileById(currentUser, id)
	}
}
