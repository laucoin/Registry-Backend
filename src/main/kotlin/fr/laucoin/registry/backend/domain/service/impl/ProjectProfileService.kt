package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.GenericProfileService
import fr.laucoin.registry.backend.domain.service.IProjectProfileService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import java.time.OffsetTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProjectProfileService(
    private val profileService: IUserProjectProfileService,
    private val repository: IProjectProfileModelRepository,
    private val roleService: IRoleService,
    private val userRepository: IUserModelRepository,
    @Value("\${registry.feature.profile.searched.max-user-result}")
    private val maxUserResult: Int,
): IProjectProfileService, GenericProfileService(repository) {
    override fun findProjectProfilesPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: ProjectProfileSearchParamModel,
    ): Mono<PageModel<ProjectProfileModel>> {
        return repository
            .findProjectProfilesPageByProjectId(projectId, pageable, searchParams)
    }

    override fun findProjectProfileById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileModel> {
        return repository.findById(projectId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun searchUsers(textSearched: String?): Flux<UserModel> {
        return userRepository.findWithLimit(maxUserResult, UserSearchParamModel(textSearched, visibilitySearched = true))
    }

    override fun getAssignableProjectRoles(currentUser: CurrentUserModel, projectId: UUID): Flux<String> {
        return repository.findProjectProfileByProjectAndUserId(
            projectId,
            currentUser.id !!,
            ProjectProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            ),
        )
            .notFoundIfEmpty(Pair(projectId, currentUser.id !!))
            .map { roleService.getAssignableProjectRoles(it) }
            .flatMapMany { Flux.fromIterable(it) }
    }

    override fun createProjectProfiles(
        currentUser: CurrentUserModel,
        projectId: UUID,
        userIds: List<UUID>,
        profiles: List<ProjectProfileModel>
    ): Mono<Pair<List<UUID>, List<UUID>>> {
        return validateNoProfileConflict(
            projectId,
            userIds,
            profileId = null,
            profiles.first().startAccess?.toZonedDateTime(OffsetTime.MIN),
            profiles.first().endAccess?.toZonedDateTime(OffsetTime.MAX)
        )
            .map { allowedUsers ->
                profiles.filter { allowedUsers.contains(it.user !!.id) }
                    .map { it.apply { create(currentUser) } }
            }
            .flatMapMany { repository.saveAll(it) }
            .collectList()
            .map {
                val savedUserId = it.mapNotNull { profile -> profile.user !!.id }
                Pair(savedUserId, userIds.minus(savedUserId.toSet()))
            }
    }

    override fun updateProjectProfileById(
        currentUser: CurrentUserModel,
        projectId: UUID,
        id: UUID,
        profile: ProjectProfileModel
    ): Mono<ProjectProfileModel> {
        return findProjectProfileById(projectId, id, visibilitySearched = null)
            .flatMap {
                validateNoProfileConflict(
                    projectId,
                    listOf(it.user !!.id !!),
                    it.id,
                    profile.endAccess?.toZonedDateTime(OffsetTime.MIN),
                    profile.endAccess?.toZonedDateTime(OffsetTime.MAX),
                ).map { _ -> it }
            }
            .validateRole(currentUser, projectId, profile)
            .validateNotLastProjectRoleLevel0(PROJECT_PROFILE_UPDATE_LAST_PROJECT_ADMINISTRATOR)
            .map {
                it.apply {
                    role = profile.role
                    startAccess = profile.startAccess
                    endAccess = profile.endAccess
                }
            }
            .updateProjectProfile(currentUser)
    }

    override fun blockProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ProjectProfileModel> {
        return findProjectProfileById(projectId, id, visibilitySearched = true)
            .validateNotLastProjectRoleLevel0(PROJECT_PROFILE_BLOCK_LAST_PROJECT_ADMINISTRATOR)
            .updateVisibility(visibility = false)
            .updateProjectProfile(currentUser)
    }

    override fun unblockProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ProjectProfileModel> {
        return findProjectProfileById(projectId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateProjectProfile(currentUser)
    }

    override fun deleteProjectProfileById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
        return findProjectProfileById(projectId, id, visibilitySearched = null)
            .validateNotLastProjectRoleLevel0(PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR)
            .flatMap { repository.deleteById(id) }
    }

    private fun Mono<ProjectProfileModel>.validateNotLastProjectRoleLevel0(error: String) = flatMap {
        profileService.validateNotLastProjectRoleLevel0(it.user !!.id !!, it.project !!.id !!, it, error)
    }

    private fun Mono<ProjectProfileModel>.validateRole(
        currentUser: CurrentUserModel, projectId: UUID, profile: ProjectProfileModel
    ) = flatMap { profileToUpdate ->
        repository.findProjectProfileByProjectAndUserId(
            projectId,
            currentUser.id !!,
            ProjectProfileSearchParamModel(
                visibilitySearched = true,
                availabilitySearched = true,
                statusSearched = listOf(ACCEPTED),
            )
        )
            .handle { it, handle ->
                val eligibleRoles = roleService.getAssignableProjectRoles(it)
                if (! eligibleRoles.contains(profileToUpdate.role)) {
                    log.warn(
                        "User \"{}\" cannot update project profile with a role higher up the breast.",
                        currentUser.id,
                    )
                    handle.error(
                        RegistryException(
                            FORBIDDEN, PROJECT_PROFILE_UPDATE_ROLE_HIGHER_THAN_CURRENT_USER, arrayListOf(profileToUpdate.role)
                        )
                    )
                } else if (! eligibleRoles.contains(profile.role)) {
                    log.warn(
                        "User \"{}\" tried to update project profile with a role higher up the breast.",
                        currentUser.id,
                    )
                    handle.error(RegistryException(FORBIDDEN, PROJECT_PROFILE_ASSIGNS_ROLE_HIGHER_THAN_CURRENT_USER))
                } else handle.next(profileToUpdate)
            }
    }

    private fun Mono<ProjectProfileModel>.updateProjectProfile(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }
}
