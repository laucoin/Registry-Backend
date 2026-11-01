package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.extension.SortExt.toSortModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.service.IUserService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IUserV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.UserRoleWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.UserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.UserRoleReaderDtoMapper
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur REST des endpoints de l'utilisateur.
 * Périmètre : Mappe les paramètres de requête et les DTO, et délègue aux cas d'usage de l'utilisateur.
 * Limites : Ne porte aucune règle métier ; l'autorisation est déclarée sur le contrat.
 */
@RestController
class UserV2Controller(
	private val service: IUserService,
	private val readerMapper: UserReaderDtoMapper,
	private val userRoleReaderMapper: UserRoleReaderDtoMapper,
): IUserV2Controller {
	override fun findUsers(
		page: Long,
		size: Int,
		query: String?,
		isVisible: Boolean?,
		role: String?,
		sort: String?,
	): Mono<PageModel<UserReaderDto>> {
		val pageable = PageableModel(page * size, size)
		val searchParams = UserSearchParamModel(query, isVisible, role, sort = sort.toSortModel(UserSearchParamModel.SORT_FIELDS))

		return service.findUsersPage(pageable, searchParams).map(readerMapper::toDtoPage)
	}

	override fun findUserById(id: UUID): Mono<UserReaderDto> {
		return service.findUserById(id, isVisible = null).map(readerMapper::toDto)
	}

	override fun getAssignableUserRoles(currentUser: CurrentUserModel): Flux<LabelDto> {
		return service.assignableUserRoles(currentUser).map(userRoleReaderMapper::toDto)
	}

	override fun updateUserRole(currentUser: CurrentUserModel, id: UUID, role: UserRoleWriterDto): Mono<UserReaderDto> {
		return service.updateUserRoleById(currentUser, id, role.role).map(readerMapper::toDto)
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
