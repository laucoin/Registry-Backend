package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IProjectProfileQueryService
import fr.laucoin.registry.backend.domain.service.IRoleService
import java.time.Clock
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Consulte les profils d'un projet : page, détail, sélecteur d'utilisateurs et rôles attribuables.
 * Périmètre : Lecture seule des profils, avec le plafond de résultats du sélecteur.
 * Limites : Ne modifie rien.
 */
@Service
class ProjectProfileQueryService(
	private val port: IProjectProfilePort,
	private val userPort: IUserPort,
	private val roleService: IRoleService,
	clock: Clock,
	@param:Value($$"${registry.feature.profile.searched.max-user-result}")
	private val maxUserResult: Int,
) : IProjectProfileQueryService, GenericService(clock) {
	override fun findProjectProfilesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>> {
		return port
			.findProjectProfilesPageByProjectId(projectId, pageable, searchParams)
	}

	override fun findProjectProfileById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?
	): Mono<ProjectProfileModel> {
		return port.findById(projectId, id, isVisible)
			.notFoundIfEmpty(id)
	}

	override fun searchUsers(query: String?): Flux<UserModel> {
		return userPort.findWithLimit(
			maxUserResult,
			UserSearchParamModel(query, isVisible = true)
		)
	}

	override fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String> {
		return port.findProjectProfileByProjectAndUserId(
			projectId,
			currentUser.id!!,
			ProjectProfileSearchParamModel(
				isVisible = true,
				isAvailable = true,
				status = listOf(ACCEPTED),
			),
		)
			.notFoundIfEmpty(Pair(projectId, currentUser.id!!))
			.map { roleService.getAssignableProjectRoles(it) }
			.flatMapMany { Flux.fromIterable(it) }
	}
}
