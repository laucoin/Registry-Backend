package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_BLOCK_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_DELETE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_IMPERSONATE_LAST_APPLICATION_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_IMPERSONATE_LAST_EVENT_ADMINISTRATOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.UserError.USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IUserModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import fr.laucoin.registry.backend.domain.service.IUserService
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(
    private val repository: IUserModelRepository,
    private val preferencesService: IPreferencesService,
    private val userEventProfileService: IUserEventProfileService,
    private val transactionalOperator: TransactionalOperator,
    private val roleService: IRoleService,
): ApplicationListener<ContextRefreshedEvent>, IUserService, GenericService<UserModel>(compareBy { it.lastName }) {
    private lateinit var serviceAccount: UserModel

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        repository.findServiceAccount()
            .subscribe { serviceAccount = it }
    }

    override fun findUsers(order: Direction, onlyVisible: Boolean, searched: String?): Flux<UserModel> {
        return repository.findAll(onlyVisible)
            .filter { isNotServiceAccount(it) }
            .searchAndSort(order, searched)
    }

    override fun findUserById(id: UUID, onlyVisible: Boolean): Mono<UserModel> {
        return repository.findById(id, onlyVisible)
            .filter { isNotServiceAccount(it) }
            .notFoundIfEmpty(id)
    }

    private fun findUserByIdWithEligibleRole(currentUser: UserModel, id: UUID, onlyVisible: Boolean): Mono<UserModel> {
        val eligibleRoles = roleService.getAssignableUserRoles(currentUser)
        return findUserById(id, onlyVisible)
            .filter { Objects.isNull(it.role) || eligibleRoles.contains(it.role) }
            .notFoundIfEmpty(id)
    }

    override fun findUserByOidcId(id: UUID, onlyVisible: Boolean): Mono<CurrentUserModel> {
        return repository.findByOidcId(id, onlyVisible)
            .filter { isNotServiceAccount(it) }
    }

    override fun getServiceAccount(): UserModel = serviceAccount

    override fun createUser(oidcId: UUID, email: String, firstName: String?, lastName: String?): Mono<CurrentUserModel> {
        log.info("Saving new user (OIDC ID \"{}\") in database", oidcId)
        val user = UserModel(
            oidcId = oidcId,
            email = email,
            firstName = firstName,
            lastName = lastName,
            role = roleService.getDefaultUserRole(),
            lastLogin = ZonedDateTime.now()
        )
        user.create(serviceAccount)

        return repository.save(user)
            .map { CurrentUserModel(it) }
            .flatMap { createdUser ->
                preferencesService.findByUser(createdUser)
                    .map {
                        createdUser.preferences = it
                        createdUser
                    }
            }
            .`as`(transactionalOperator::transactional)
    }

    private fun updateUser(currentUser: UserModel, user: UserModel): Mono<UserModel> {
        return repository.save(user.apply { update(currentUser) })
    }

    override fun updateUserIfPersonalDataChanged(
        user: CurrentUserModel,
        email: String,
        firstName: String?,
        lastName: String?
    ): Mono<CurrentUserModel> {
        if (user.personalDataChanged(email, firstName, lastName)) {
            log.info("Updating personal data for user \"{}\"", user.id)
            user.email = email
            user.firstName = firstName
            user.lastName = lastName
        }

        val now: ZonedDateTime = ZonedDateTime.now()
        log.info("Updating user \"{}\" last sign in date and time to {}", user.id, now)
        user.lastLogin = now

        return updateUser(serviceAccount, user)
            .map { user }
    }

    override fun updateUserRoleById(currentUser: UserModel, id: UUID, role: String?): Mono<UserModel> {
        return findUserByIdWithEligibleRole(currentUser, id, onlyVisible = true)
            .validateRole(currentUser, role, USER_ASSIGNS_ROLE_HIGHER_THAN_ITS_OWN)
            .validateNotLastRoleLevel0(USER_UPDATE_LAST_APPLICATION_ADMINISTRATOR_ROLE)
            .flatMap {
                it.role = role
                updateUser(currentUser, it)
            }
    }

    override fun blockUserById(currentUser: UserModel, id: UUID): Mono<UserModel> {
        return findUserByIdWithEligibleRole(currentUser, id, onlyVisible = true)
            .validateNotLastRoleLevel0(USER_BLOCK_LAST_APPLICATION_ADMINISTRATOR)
            .validateNotLastEventRoleLevel0(USER_BLOCK_LAST_EVENT_ADMINISTRATOR)
            .updateVisibility(visibility = false)
            .flatMap { updateUser(currentUser, it) }
    }

    override fun unblockUserById(currentUser: UserModel, id: UUID): Mono<UserModel> {
        return findUserByIdWithEligibleRole(currentUser, id, onlyVisible = false)
            .updateVisibility(visibility = true)
            .flatMap { updateUser(currentUser, it) }
    }

    override fun impersonateUserById(currentUser: UserModel, id: UUID): Mono<UserModel> {
        return findUserByIdWithEligibleRole(currentUser, id, onlyVisible = false)
            .validateNotLastRoleLevel0(USER_IMPERSONATE_LAST_APPLICATION_ADMINISTRATOR)
            .validateNotLastEventRoleLevel0(USER_IMPERSONATE_LAST_EVENT_ADMINISTRATOR)
            .flatMap {
                it.impersonate()
                updateUser(currentUser, it)
            }
    }

    override fun deleteUserById(currentUser: UserModel, id: UUID): Mono<Void> {
        return findUserByIdWithEligibleRole(currentUser, id, onlyVisible = false)
            .validateNotLastRoleLevel0(USER_DELETE_LAST_APPLICATION_ADMINISTRATOR)
            .validateNotLastEventRoleLevel0(USER_DELETE_LAST_EVENT_ADMINISTRATOR)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<UserModel>.validateNotLastRoleLevel0(error: String) = flatMap { userToUpdate ->
        val roleLevel = roleService.getLevelByUserRole(userToUpdate.role)
        if (Objects.isNull(roleLevel) || roleLevel !! > 0) {
            return@flatMap Mono.just(userToUpdate)
        }

        repository.findByRoleLevel(roleLevel = 0, onlyVisible = true)
            .filter { userToUpdate.id != it.id }
            .collectList()
            .handle { it, handle ->
                if (it.isEmpty()) {
                    log.warn("The user {} is the last administrator of the application", userToUpdate.id)
                    handle.error(RegistryExceptionModel(FORBIDDEN, error))
                } else handle.next(userToUpdate)
            }
    }

    private fun Mono<UserModel>.validateNotLastEventRoleLevel0(error: String) = flatMap {
        userEventProfileService.validateNotLastEventRoleLevel0(it.id !!, eventId = null, it, error)
    }

    private fun isNotServiceAccount(user: UserModel): Boolean = user.id != serviceAccount.id

    private fun Mono<UserModel>.validateRole(currentUser: UserModel, role: String?, error: String) = handle { it, handle ->
        val eligibleRoles = roleService.getAssignableUserRoles(currentUser)
        if (Objects.nonNull(role) && ! eligibleRoles.contains(role)) {
            log.warn("The role {} is not assignable by the user {}", role, currentUser.id)
            handle.error(RegistryExceptionModel(FORBIDDEN, error))
        } else handle.next(it)
    }
}
