package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import reactor.core.publisher.Mono

open class GenericProfileService(
    private val repository: IProjectProfileModelRepository,
): GenericService() {
    protected fun validateNoProfileConflict(
        projectId: UUID,
        userIds: List<UUID>,
        profileId: UUID?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?
    ): Mono<List<UUID>> {
        return repository.findUserIdsWithProjectProfileForProjectWithProfileExclusion(
            projectId,
            userIds,
            profileId,
            statusSearched = listOf(ACCEPTED, INVITED),
            startDateTimeSearched = startAccess,
            endDateTimeSearched = endAccess,
        )
            .collectList()
            .handle { userIdsWithConflictualProfile, handle ->
                when {
                    userIdsWithConflictualProfile.size == userIds.size -> {
                        log.warn("Another profile already exist for the user(s) \"{}\" on the project \"{}\".", userIds, projectId)
                        handle.error(RegistryException(CONFLICT, PROJECT_PROFILE_ALREADY_EXIST_ON_RANGE))
                    }

                    userIdsWithConflictualProfile.isNotEmpty() -> {
                        log.warn(
                            "Partial request because, profile already exist for the user(s) \"{}\" on the project \"{}\".",
                            userIds.filter { userIdsWithConflictualProfile.contains(it) },
                            projectId
                        )
                        handle.next(userIds.filter { ! userIdsWithConflictualProfile.contains(it) })
                    }

                    else -> handle.next(userIds)
                }
            }
    }
}
