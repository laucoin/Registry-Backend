package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IUserProjectProfileV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.SortedPageQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PageReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.PageQueryDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PageReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileReaderDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserProjectProfileV2Controller(
	private val service: IUserProjectProfileService,
	private val readerMapper: ProjectProfileReaderDtoMapper,
	private val pageQueryMapper: PageQueryDtoMapper,
	private val sortParamMapper: SortParamDtoMapper,
	private val pageReaderMapper: PageReaderDtoMapper,
): IUserProjectProfileV2Controller {
	override fun findUserProjectProfiles(
		currentUser: CurrentUserModel,
		page: SortedPageQueryDto,
		q: String?,
		available: Boolean?,
		status: ProfileStatusEnum?,
		dateTime: ZonedDateTime?,
	): Mono<PageReaderDto<ProjectProfileReaderDto>> {
		val pageable = pageQueryMapper.toPageable(page)
		val sortFields = sortParamMapper.toSortModels(page.sort, page.direction) { key ->
			ProjectProfileSortFieldEnum.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
		}
		val searchParams = ProjectProfileSearchParamModel(q, available, status, dateTime)

		return service.findProjectProfilesPage(currentUser.id!!, pageable, searchParams, sortFields)
			.map { pageReaderMapper.toDto(it, readerMapper::toDto) }
	}

	override fun acceptUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectProfileReaderDto> {
		return service.updateUserProjectProfileStatusById(currentUser, id, ACCEPTED).map(readerMapper::toDto)
	}

	override fun rejectUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectProfileReaderDto> {
		return service.updateUserProjectProfileStatusById(currentUser, id, REJECTED).map(readerMapper::toDto)
	}

	override fun createSupportProjectProfile(currentUser: CurrentUserModel, projectId: UUID): Mono<ProjectProfileReaderDto> {
		return service.createSupportProjectProfile(currentUser, projectId).map(readerMapper::toDto)
	}

	override fun deleteUserProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserProjectProfileById(currentUser, id)
	}
}
