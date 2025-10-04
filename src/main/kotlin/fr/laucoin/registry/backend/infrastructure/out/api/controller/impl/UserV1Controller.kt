package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IUserV1Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.UserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.UserRoleReaderDtoMapper
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class UserV1Controller(
	private val service: IUserService,
	private val readerMapper: UserReaderDtoMapper,
	private val userRoleReaderMapper: UserRoleReaderDtoMapper,
): IUserV1Controller {
	override fun findUsers(
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?
	): Mono<PageModel<UserReaderDto>> {
		val pageable = PageableModel(pageNumber * pageSize, pageSize)
		val searchParams = UserSearchParamModel(textSearched, visibilitySearched)

		return service.findUsersPage(pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findUserById(id: UUID): Mono<UserReaderDto> {
		return service.findUserById(id, visibilitySearched = null).map(readerMapper::toDto)
	}

	override fun getAssignableUserRoles(currentUser: CurrentUserModel): Flux<LabelDto> {
		return service.assignableUserRoles(currentUser).map(userRoleReaderMapper::toDto)
	}

	override fun updateUserRole(currentUser: CurrentUserModel, id: UUID, role: String?): Mono<UserReaderDto> {
		return service.updateUserRoleById(currentUser, id, role).map(readerMapper::toDto)
	}

	override fun blockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserReaderDto> {
		return service.blockUserById(currentUser, id).map(readerMapper::toDto)
	}

	override fun unblockUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserReaderDto> {
		return service.unblockUserById(currentUser, id).map(readerMapper::toDto)
	}

	override fun impersonateUserById(currentUser: CurrentUserModel, id: UUID): Mono<UserReaderDto> {
		return service.impersonateUserById(currentUser, id).map(readerMapper::toDto)
	}

	override fun impersonateCurrentUser(currentUser: CurrentUserModel): Mono<UserReaderDto> {
		return service.impersonateUserById(currentUser, currentUser.id!!).map(readerMapper::toDto)
	}

	override fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserById(currentUser, id)
	}
}
