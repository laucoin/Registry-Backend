package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.PROJECT
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.core.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.core.IUserService
import fr.laucoin.registry.backend.domain.service.usecase.IProjectProfileUseCase
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

/**
 * Objectif : Invite des utilisateurs à un projet à partir de leurs adresses e-mail, dans une même transaction.
 * Périmètre : Verrouille le projet, résout les utilisateurs (en créant les inconnus en attente) puis crée leurs profils.
 * Limites : Ne porte aucune règle propre aux utilisateurs ni aux profils ; elles restent dans leurs services.
 */
@Service
class ProjectProfileUseCase(
	private val profileService: IProjectProfileService,
	private val userService: IUserService,
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
) : IProjectProfileUseCase {
	override fun createProjectProfiles(
		currentUser: CurrentUserModel,
		projectId: UUID,
		profiles: List<ProjectProfileModel>,
	): Mono<Pair<List<String>, List<String>>> {
		val emails = profiles.map { it.user!!.email!! }

		return rowLockPort.lock(PROJECT, projectId)
			.then(Mono.defer { userService.resolveUsersByEmail(emails) })
			.flatMap { users -> profileService.createProjectProfiles(currentUser, projectId, profiles, users) }
			.`as`(transactionalOperator::transactional)
	}
}
