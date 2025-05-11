package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.domain.service.GenericProfileService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserProjectProfileService
import java.time.OffsetTime
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

@Service
class UserProjectProfileService(
    private val repository: IProjectProfileModelRepository,
    private val roleService: IRoleService,
    private val preferencesRepository: IPreferencesModelRepository,
    private val transactionalOperator: TransactionalOperator,
): IUserProjectProfileService, GenericProfileService(repository) {
    override fun findProjectProfilesPage(
        userId: UUID,
        pageable: PageableModel,
        searchParams: ProjectProfileSearchParamModel,
    ): Mono<PageModel<ProjectProfileModel>> {
        return repository
            .findProjectProfilesPageByUserId(userId, pageable, searchParams)
    }

    override fun <T: GenericModel> validateNotLastProjectRoleLevel0(
        userId: UUID,
        projectId: UUID?,
        result: T,
        error: String
    ): Mono<T> {
        return repository.findLevel0ProjectProfileRoleByUserId(userId, visibilitySearched = true)
            .filter { Objects.isNull(projectId) || it.project?.id !== projectId }
            .collectList()
            .handle { it, handle ->
                if (it.isNotEmpty()) {
                    log.warn("The user {} is the last administrator of {} project(s)", userId, it.size)
                    handle.error(RegistryException(FORBIDDEN, error, arrayListOf(it.first().project?.name)))
                } else handle.next(result)
            }
    }

    override fun createUserProjectProfileFromProject(currentUser: CurrentUserModel, project: ProjectModel): Mono<ProjectProfileModel> {
        val profile = ProjectProfileModel().apply {
            this.project = project
            this.user = currentUser
            this.role = roleService.getLevel0RoleFromProjectRoles()
            this.status = ACCEPTED
        }
        profile.create(currentUser)

        return repository.create(profile)
            .flatMap { newProfile ->
                preferencesRepository.findByUserId(currentUser.id !!, visibilitySearched = null)
                    .flatMap {
                        if (Objects.isNull(it.selectedProfile)) {
                            it.selectedProfile = newProfile
                            preferencesRepository.save(it).thenReturn(newProfile)
                        } else Mono.just(newProfile)
                    }
            }
            .`as`(transactionalOperator::transactional)
    }

    override fun updateUserProjectProfileStatusById(
        currentUser: CurrentUserModel,
        id: UUID,
        status: ProfileStatusEnum
    ): Mono<ProjectProfileModel> {
        return repository.findProjectProfileByUserIdAndId(currentUser.id !!, id, visibilitySearched = true)
            .filter { it.status == INVITED }
            .notFoundIfEmpty(id)
            .flatMap { profile ->
                profile.status = status
                profile.update(currentUser)
                repository.update(profile)
            }
    }

    override fun createSupportProjectProfile(currentUser: CurrentUserModel, projectId: UUID): Mono<ProjectProfileModel> {
        val now = CustomDateTimeModel.now()
        val profile = ProjectProfileModel().apply {
            user = currentUser
            project = ProjectModel().apply { id = projectId }
            role = roleService.getLevel0RoleFromProjectRoles()
            status = ACCEPTED
            startAccess = now
            endAccess = now.apply { time !!.plusHours(1) }
            create(currentUser)
        }

        return validateNoProfileConflict(
            projectId,
            listOf(currentUser.id !!),
            profileId = null,
            profile.startAccess !!.toZonedDateTime(OffsetTime.MIN),
            profile.endAccess !!.toZonedDateTime(OffsetTime.MAX),
        ).flatMap { repository.create(profile) }
    }

    override fun deleteUserProjectProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Void> {
        return repository.findProjectProfileByUserIdAndId(currentUser.id !!, id, visibilitySearched = null)
            .flatMap {
                validateNotLastProjectRoleLevel0(
                    it.user !!.id !!,
                    it.project !!.id !!,
                    it,
                    PROJECT_PROFILE_DELETE_LAST_PROJECT_ADMINISTRATOR
                )
            }
            .flatMap { repository.deleteById(id) }
    }
}
