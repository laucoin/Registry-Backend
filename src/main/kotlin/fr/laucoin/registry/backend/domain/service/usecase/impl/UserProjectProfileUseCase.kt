package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.core.IProjectService
import fr.laucoin.registry.backend.domain.service.core.IUserProjectProfileService
import fr.laucoin.registry.backend.domain.service.usecase.IUserProjectProfileUseCase
import java.time.OffsetTime
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des profils de projet de l'utilisateur courant pour la couche REST.
 * Périmètre : Délègue au service UserProjectProfile et orchestre la création d'un profil de support (verrou du projet, conflit de plage, enregistrement, projet renvoyé) dans une même transaction.
 * Limites : Ne porte aucune règle métier ; elles restent dans les services.
 */
@Service
class UserProjectProfileUseCase(
	private val service: IUserProjectProfileService,
	private val profileService: IProjectProfileService,
	private val projectService: IProjectService,
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
) : IUserProjectProfileUseCase {
	override fun findProjectProfilesPage(
		userId: UUID,
		pageable: PageableModel,
		searchParams: ProjectProfileSearchParamModel,
	): Mono<PageModel<ProjectProfileModel>> {
		return service.findProjectProfilesPage(userId, pageable, searchParams)
	}

	override fun findProjectProfilesById(currentUser: CurrentUserModel, id: UUID): Mono<ProjectProfileModel> {
		return service.findProjectProfilesById(currentUser, id)
	}

	override fun updateUserProjectProfileStatusById(
		currentUser: CurrentUserModel,
		id: UUID,
		status: ProfileStatusEnum,
	): Mono<ProjectProfileModel> {
		return service.updateUserProjectProfileStatusById(currentUser, id, status)
	}

	override fun createSupportProjectProfile(currentUser: CurrentUserModel, projectId: UUID): Mono<ProjectModel> {
		val profile = service.supportProjectProfile(currentUser, projectId)
		return rowLockPort.lock(PROJECT, projectId)
			.then(Mono.defer { validateNoSupportConflict(currentUser, projectId, profile) })
			.then(Mono.defer { service.createSupportProjectProfile(currentUser, profile) })
			.`as`(transactionalOperator::transactional)
			.then(Mono.defer { projectService.findProjectWithProfileById(currentUser, projectId) })
	}

	private fun validateNoSupportConflict(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profile: ProjectProfileModel,
	): Mono<List<UUID>> {
		return profileService.validateNoProfileConflict(
			projectId,
			listOf(currentUser.id!!),
			profileId = null,
			profile.startAccess!!.toZonedDateTime(OffsetTime.MIN),
			profile.endAccess!!.toZonedDateTime(OffsetTime.MAX),
		)
	}

	override fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Unit> {
		return service.deleteUserProjectProfileById(currentUser, id)
	}
}
