package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.ReactiveCacheService
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_LOGIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserQueries.NOT_SERVICE_ACCOUNT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserQueries.SELECT_USER_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserQueries.USER_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.orderByWithRelevance
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IUserEntityRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@Service
class UserModelPostgresRepository(
	private val repository: IUserEntityRepository,
	private val mapper: UserEntityMapper,
	private val currentUserMapper: CurrentUserEntityMapper,
	private val databaseClient: DatabaseClient,
	private val converter: MappingR2dbcConverter,
	@param:Value($$"${registry.performance.count-cache-ttl-seconds:30}")
	private val countCacheTtlSeconds: Long,
) : IUserPort {
	/**
	 * ADR 018 §5 — the hot-list count is the second query of every page request;
	 * within the TTL window it is served from memory. Exactness within the TTL
	 * is deliberately traded away; user writes evict so local changes show
	 * immediately on the writing replica.
	 */
	private val countCache =
		ReactiveCacheService<String, Long>(Duration.ofSeconds(countCacheTtlSeconds.coerceAtLeast(1)))

	override fun findPage(
		pageable: PageableModel,
		searchParams: UserSearchParamModel,
		sort: List<SortModel<UserSortFieldEnum>>,
	): Mono<PageModel<UserModel>> {
		val entities = if (sort.isEmpty()) {
			repository.findAll(
				searchParams.textSearched,
				searchParams.visibilitySearched,
				pageable.limit,
				pageable.offset,
			)
		} else {
			findAllSorted(searchParams, pageable, sort)
		}

		return Mono.zip(
			countUsers(searchParams),
			entities.map(mapper::toModel).collectList()
		).map {
			PageModel(pageable, it.t1, it.t2)
		}
	}

	/**
	 * UserSearchParamModel equality only covers its primary constructor, so the
	 * cache key is built explicitly from both criteria.
	 */
	private fun countUsers(searchParams: UserSearchParamModel): Mono<Long> {
		if (countCacheTtlSeconds <= 0) {
			return repository.countAll(searchParams.textSearched, searchParams.visibilitySearched)
		}
		val key = "${searchParams.textSearched}|${searchParams.visibilitySearched}"
		return countCache.get(key) {
			repository.countAll(searchParams.textSearched, searchParams.visibilitySearched)
		}
	}

	/**
	 * API v2 sorted page (ADR 017 §5). The ORDER BY is built exclusively from
	 * the [UserSortFieldEnum] whitelist ([toColumn]) — user input never reaches
	 * the SQL string. Row mapping reuses the same converter Spring Data applies
	 * to the annotated queries.
	 */
	private fun findAllSorted(
		searchParams: UserSearchParamModel,
		pageable: PageableModel,
		sort: List<SortModel<UserSortFieldEnum>>,
	): Flux<UserEntity> {
		val orderBy = orderByWithRelevance(
			searchParams.textSearched,
			sort.joinToString(", ") { "t.${it.field.toColumn()} ${if (it.descending) "DESC" else "ASC"}" },
		)
		val sql = """
        SELECT t.*, $SELECT_USER_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_SERVICE_ACCOUNT AND $USER_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE
        ORDER BY $orderBy
        LIMIT :limit OFFSET :offset
        """

		var spec = databaseClient.sql(sql)
			.bind("limit", pageable.limit)
			.bind("offset", pageable.offset)
		spec = searchParams.textSearched
			?.let { spec.bind("textSearched", it) }
			?: spec.bindNull("textSearched", String::class.java)
		spec = searchParams.visibilitySearched
			?.let { spec.bind("visibilitySearched", it) }
			?: spec.bindNull("visibilitySearched", Boolean::class.javaObjectType)

		return spec
			.map { row, metadata -> converter.read(UserEntity::class.java, row, metadata) }
			.all()
	}

	private fun UserSortFieldEnum.toColumn(): String = when (this) {
		UserSortFieldEnum.FIRST_NAME -> USER_FIRST_NAME
		UserSortFieldEnum.LAST_NAME -> USER_LAST_NAME
		UserSortFieldEnum.EMAIL -> USER_EMAIL
		UserSortFieldEnum.ROLE -> USER_ROLE
		UserSortFieldEnum.LAST_LOGIN -> USER_LAST_LOGIN
	}

	override fun findWithLimit(limit: Int, searchParams: UserSearchParamModel): Flux<UserModel> {
		return repository.findWithLimit(
			searchParams.textSearched,
			searchParams.visibilitySearched,
			limit,
		).map(mapper::toModel)
	}

	override fun findById(id: UUID, visibilitySearched: Boolean?): Mono<UserModel> {
		return repository.findById(id, visibilitySearched)
			.map(mapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun findByOidcId(oidcId: UUID, visibilitySearched: Boolean?): Mono<CurrentUserModel> {
		return repository.findByOidcId(oidcId, visibilitySearched)
			.map(currentUserMapper::toModel)
			.switchIfEmpty(Mono.empty())
	}

	override fun findByEmail(email: String, visibilitySearched: Boolean?): Flux<CurrentUserModel> {
		return repository.findByEmail(email, visibilitySearched)
			.map(currentUserMapper::toModel)
	}

	override fun findServiceAccount(): Mono<CurrentUserModel> =
		repository.findServiceAccount().map(currentUserMapper::toModel)

	override fun findByRoleLevel(roleLevel: Int, visibilitySearched: Boolean?): Flux<UserModel> {
		return repository.findByRoleLevel(roleLevel, visibilitySearched).map(mapper::toModel)
	}

	override fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findUserIdsOlderThanLastLogin(dateThreshold)
	}

	override fun findLightUserIdsOlderThanCreation(dateThreshold: LocalDate): Flux<UUID> {
		return repository.findLightUserIdsOlderThanCreation(dateThreshold)
	}

	override fun create(element: UserModel): Mono<UserModel> {
		return save(element)
	}

	override fun update(element: UserModel): Mono<UserModel> {
		return save(element)
	}

	/**
	 * Any user write can change list membership (visibility, purge flags) —
	 * evict the count cache so the writing replica reflects it immediately
	 * (ADR 018 §3).
	 */
	private fun save(element: UserModel): Mono<UserModel> {
		return repository.save(mapper.toEntity(element)).map(mapper::toModel)
			.doOnNext { countCache.evictAll() }
	}

	override fun deleteById(id: UUID): Mono<Unit> {
		return repository.deleteById(id).thenReturn(Unit)
			.doOnNext { countCache.evictAll() }
	}
}
