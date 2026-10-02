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
 * Common base for services that create or move Profiles into an active state: guards against two
 * ACCEPTED/INVITED Profiles overlapping for the same User on the same Project, rejecting the whole
 * batch on a full conflict or filtering out only the conflicting Users on a partial one.
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
			startDateTime = startAccess,
			endDateTime = endAccess,
		)
			.collectList()
			.handle { userIdsWithConflictualProfile, handle ->
				when {
					userIdsWithConflictualProfile.size == userIds.size -> {
						log.warn(
							"Another profile already exist for the user(s) \"{}\" on the project \"{}\".",
							userIds,
							projectId
						)
						handle.error(RegistryException(CONFLICT, PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE))
					}

					userIdsWithConflictualProfile.isNotEmpty() -> {
						log.warn(
							"Partial request because, profile already exist for the user(s) \"{}\" on the project \"{}\".",
							userIds.filter { userIdsWithConflictualProfile.contains(it) },
							projectId
						)
						handle.next(userIds.filter { !userIdsWithConflictualProfile.contains(it) })
					}

					else -> handle.next(userIds)
				}
			}
	}
}
