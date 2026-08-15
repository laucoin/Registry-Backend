package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IUserV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.UserWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.SortParamDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.UserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.UserRoleReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
class UserV2Controller(
	private val service: IUserService,
	private val readerMapper: UserReaderDtoMapper,
	private val userRoleReaderMapper: UserRoleReaderDtoMapper,
) : IUserV2Controller {
	override fun findUsers(
		page: Int,
		size: Int,
		sort: List<String>?,
		direction: String,
		q: String?,
		visible: Boolean?,
	): Mono<PageModel<UserReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = UserSearchParamModel(q, visible)
		val sortModels = SortParamDtoMapper.toSortModels(sort, direction, UserSortFieldEnum::fromParamName)

		return service.findUsersPage(pageable, searchParams, sortModels).map(readerMapper::toDtoPage)
	}

	override fun findUserById(id: UUID): Mono<UserReaderDto> {
		return service.findUserById(id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun getAssignableUserRoles(currentUser: CurrentUserModel): Flux<LabelDto> {
		return service.assignableUserRoles(currentUser).map(userRoleReaderMapper::toDto)
	}

	override fun updateUser(currentUser: CurrentUserModel, id: UUID, user: UserWriterDto): Mono<UserReaderDto> {
		return service.updateUserRoleById(currentUser, id, user.role).map(readerMapper::toDto)
	}

	override fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserReaderDto> {
		return service.blockUserById(currentUser, id).map(readerMapper::toDto)
	}

	override fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserReaderDto> {
		return service.unblockUserById(currentUser, id).map(readerMapper::toDto)
	}

	override fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserById(currentUser, id)
	}

	override fun deleteCurrentUser(currentUser: CurrentUserModel): Mono<Unit> {
		return service.deleteCurrentUser(currentUser)
	}
}
