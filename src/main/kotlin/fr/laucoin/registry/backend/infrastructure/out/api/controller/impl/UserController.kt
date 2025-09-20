package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IUserController
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.UserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.UserRoleReaderDtoMapper
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class UserController(
	private val service: IUserService,
	private val readerMapper: UserReaderDtoMapper,
	private val userRoleReaderMapper: UserRoleReaderDtoMapper,
): IUserController {
	override fun findUsers(
		locale: Locale,
		pageNumber: Int,
		pageSize: Int,
		textSearched: String?,
		visibilitySearched: Boolean?
	): Mono<PageModel<UserReaderDto>> {
		return service.findUsersPage(
			PageableModel(pageNumber * pageSize, pageSize),
			UserSearchParamModel(textSearched, visibilitySearched)
		)
			.map { readerMapper.toDtoPage(it, locale) }
	}

	override fun findUserById(locale: Locale, id: UUID): Mono<UserReaderDto> {
		return service.findUserById(id, visibilitySearched = null)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun getAssignableUserRoles(currentUser: CurrentUserModel, locale: Locale): Flux<LabelDto> {
		return service.assignableUserRoles(currentUser)
			.map { userRoleReaderMapper.toDto(it, locale) }
	}

	override fun updateUserRole(
		currentUser: CurrentUserModel,
		locale: Locale,
		id: UUID,
		role: String?
	): Mono<UserReaderDto> {
		return service.updateUserRoleById(currentUser, id, role)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun blockUserById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<UserReaderDto> {
		return service.blockUserById(currentUser, id)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun unblockUserById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<UserReaderDto> {
		return service.unblockUserById(currentUser, id)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun impersonateUserById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<UserReaderDto> {
		return service.impersonateUserById(currentUser, id)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun impersonateCurrentUser(currentUser: CurrentUserModel, locale: Locale): Mono<UserReaderDto> {
		return service.impersonateUserById(currentUser, currentUser.id!!)
			.map { readerMapper.toDto(it, locale) }
	}

	override fun deleteUserById(currentUser: CurrentUserModel, id: UUID): Mono<Void> {
		return service.deleteUserById(currentUser, id)
	}
}
