package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import reactor.core.publisher.Mono

/**
 * Purpose: Shares the profile conflict check between profile services.
 * Scope: Detects users who already hold an overlapping profile on a project.
 * Limits: Does not create or update profiles.
 */
open class GenericProfileService(
	private val repository: IProjectProfilePort,
): GenericService() {
	protected fun validateNoProfileConflict(
		projectId: UUID,
		userIds: List<UUID>,
		profileId: UUID?,
		startAccess: ZonedDateTime?,
		endAccess: ZonedDateTime?
	): Mono<List<UUID>> {
		return repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
			projectId,
			userIds,
			profileId,
			status = listOf(ACCEPTED, INVITED),
			from = startAccess,
			to = endAccess,
		)
			.collectList()
			.handle { conflicting, handle ->
				if (conflicting.size == userIds.size) {
					log.warn(
						"Another profile already exist for the user(s) \"{}\" on the project \"{}\".",
						userIds,
						projectId
					)
					handle.error(RegistryException(CONFLICT, PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE))
				} else {
					handle.next(usersWithoutConflict(projectId, userIds, conflicting))
				}
			}
	}

	private fun usersWithoutConflict(projectId: UUID, userIds: List<UUID>, conflicting: List<UUID>): List<UUID> {
		if (conflicting.isEmpty()) return userIds

		log.warn(
			"Partial request because, profile already exist for the user(s) \"{}\" on the project \"{}\".",
			userIds.filter { conflicting.contains(it) },
			projectId
		)
		return userIds.filter { !conflicting.contains(it) }
	}
}
