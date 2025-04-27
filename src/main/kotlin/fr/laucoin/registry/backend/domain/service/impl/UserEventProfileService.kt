package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventProfileError.EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.service.GenericProfileService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.time.LocalTime
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

@Service
class UserEventProfileService(
    private val repository: IEventProfileModelRepository,
    private val roleService: IRoleService,
    private val preferencesRepository: IPreferencesModelRepository,
    private val transactionalOperator: TransactionalOperator,
): IUserEventProfileService, GenericProfileService(repository) {
    override fun findEventProfilesPage(
        userId: UUID,
        pageable: PageableModel,
        searchParams: EventProfileSearchParamModel,
    ): Mono<PageModel<EventProfileModel>> {
        return repository
            .findEventProfilesPageByUserId(userId, pageable, searchParams)
    }

    override fun findUserEventProfileById(
        currentUser: CurrentUserModel,
        id: UUID,
        visibilitySearched: Boolean?
    ): Mono<EventProfileModel> {
        return repository.findEventProfileByUserIdAndId(currentUser.id !!, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun <T: GenericModel> validateNotLastEventRoleLevel0(userId: UUID, eventId: UUID?, result: T, error: String): Mono<T> {
        return repository.findLevel0EventProfileRoleByUserId(userId, visibilitySearched = true)
            .filter { Objects.isNull(eventId) || it.event?.id !== eventId }
            .collectList()
            .handle { it, handle ->
                if (it.isNotEmpty()) {
                    log.warn("The user {} is the last administrator of {} event(s)", userId, it.size)
                    handle.error(RegistryException(FORBIDDEN, error, arrayListOf(it.first().event?.name)))
                } else handle.next(result)
            }
    }

    override fun createUserEventProfileFromEvent(currentUser: CurrentUserModel, event: EventModel): Mono<EventProfileModel> {
        val profile = EventProfileModel().apply {
            this.event = event
            this.user = currentUser
            this.role = roleService.getLevel0RoleFromEventRoles()
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

    override fun updateUserEventProfileStatusById(
        currentUser: CurrentUserModel,
        id: UUID,
        status: ProfileStatusEnum
    ): Mono<EventProfileModel> {
        return repository.findEventProfileByUserIdAndId(currentUser.id !!, id, visibilitySearched = true)
            .filter { it.status == INVITED }
            .notFoundIfEmpty(id)
            .flatMap { profile ->
                profile.status = status
                profile.update(currentUser)
                repository.update(profile)
            }
    }

    override fun createSupportEventProfile(currentUser: CurrentUserModel, eventId: UUID): Mono<EventProfileModel> {
        val now = CustomDateTimeModel.now()
        val profile = EventProfileModel().apply {
            user = currentUser
            event = EventModel().apply { id = eventId }
            role = roleService.getLevel0RoleFromEventRoles()
            status = ACCEPTED
            startAccess = now
            endAccess = now.apply { time !!.plusHours(1) }
            create(currentUser)
        }

        return validateNoProfileConflict(
            eventId,
            listOf(currentUser.id !!),
            profileId = null,
            profile.startAccess !!.toLocalDateTime(LocalTime.MIN),
            profile.endAccess !!.toLocalDateTime(LocalTime.MAX),
        ).flatMap { repository.create(profile) }
    }

    override fun deleteUserEventProfileById(currentUser: CurrentUserModel, id: UUID): Mono<Void> {
        return repository.findEventProfileByUserIdAndId(currentUser.id !!, id, visibilitySearched = null)
            .flatMap {
                validateNotLastEventRoleLevel0(it.user !!.id !!, it.event !!.id !!, it, EVENT_PROFILE_DELETE_LAST_EVENT_ADMINISTRATOR)
            }
            .flatMap { repository.deleteById(id) }
    }
}
