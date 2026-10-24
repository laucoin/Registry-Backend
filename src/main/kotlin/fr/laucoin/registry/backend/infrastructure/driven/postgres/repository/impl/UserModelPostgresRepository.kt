package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.toPageModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IUserEntityRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port de l'utilisateur.
 * Périmètre : Délègue au repository d'entités et mappe les entités vers les modèles du domaine.
 * Limites : Ne porte aucune règle métier.
 */
@Service
class UserModelPostgresRepository(
	private val repository: IUserEntityRepository,
	private val mapper: UserEntityMapper,
	private val currentUserMapper: CurrentUserEntityMapper,
): IUserPort {
	override fun findPage(pageable: PageableModel, searchParams: UserSearchParamModel): Mono<PageModel<UserModel>> {
		return repository.findAll(
			searchParams.query,
			searchParams.isVisible,
			searchParams.role,
			searchParams.sort?.field,
			searchParams.sort?.isDescending,
			pageable.limit,
			pageable.offset,
		).toPageModel(pageable, UserEntity::fullCount, mapper::toModel)
	}

	override fun findWithLimit(limit: Int, searchParams: UserSearchParamModel): Flux<UserModel> {
		return repository.findWithLimit(
			searchParams.query,
			searchParams.isVisible,
			limit,
		).map(mapper::toModel)
	}

	override fun findById(id: UUID, isVisible: Boolean?): Mono<UserModel> {
		return repository.findById(id, isVisible)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun findByOidcId(oidcId: UUID, isVisible: Boolean?): Mono<CurrentUserModel> {
		return repository.findByOidcId(oidcId, isVisible)
			.map(currentUserMapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun findByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserModel> {
		return repository.findByEmail(email, isVisible)
			.map(currentUserMapper::toModel)
	}

	override fun findServiceAccount(): Mono<CurrentUserModel> =
		repository.findServiceAccount().map(currentUserMapper::toModel)

	override fun findByRoleLevel(roleLevel: Int, isVisible: Boolean?): Flux<UserModel> {
		return repository.findByRoleLevel(roleLevel, isVisible).map(mapper::toModel)
	}

	override fun lockLevel0Users(): Flux<UUID> = repository.lockLevel0Users()

	override fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUserIdsOlderThanLastLogin(dateThreshold)
	}

	override fun create(element: UserModel): Mono<UserModel> {
		return save(element)
	}

	override fun update(element: UserModel): Mono<UserModel> {
		return save(element)
	}

	private fun save(element: UserModel): Mono<UserModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
	}
}
