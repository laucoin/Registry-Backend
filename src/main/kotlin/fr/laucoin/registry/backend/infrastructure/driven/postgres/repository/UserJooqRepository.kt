package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.CREATED_DATE
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.EMAIL
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.FIRST_NAME
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_LOGIN
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_MODIFIED_DATE
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_NAME
import fr.laucoin.registry.backend.domain.enumeration.UserTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.DESC
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PREFERENCES
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_PROFILE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.GenericColumns
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.OrderField
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class UserJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(
		TB_USER.ID,
		TB_USER.VISIBLE,
		TB_USER.CREATED_DATE,
		TB_USER.CREATED_BY,
		TB_USER.LAST_MODIFIED_DATE,
		TB_USER.LAST_MODIFIED_BY
	)

	private fun notPurgedAndNotServiceAccount(): Condition =
		TB_USER.PURGED.isFalse.and(TB_USER.TYPE.ne(UserTypeEnum.SERVICE_ACCOUNT))

	private fun searchCondition(textSearched: String?): Condition =
		textSearched?.let { similarity(TB_USER.SEARCH_TEXT, DSL.`val`(it)).gt(0f) } ?: DSL.noCondition()

	private fun UserSortFieldEnum.toJooqField(): Field<*> = when (this) {
		FIRST_NAME -> TB_USER.FIRST_NAME
		LAST_NAME -> TB_USER.LAST_NAME
		EMAIL -> TB_USER.EMAIL
		LAST_LOGIN -> TB_USER.LAST_LOGIN
		CREATED_DATE -> TB_USER.CREATED_DATE
		LAST_MODIFIED_DATE -> TB_USER.LAST_MODIFIED_DATE
	}

	// similarityScore sorts first (a no-op constant when there's no text search), TB_USER.LAST_NAME
	// always stays last as the final tiebreaker (v1's sole, implicit order).
	private fun orderFields(similarityScore: Field<Float>, sortFields: List<SortModel<UserSortFieldEnum>>): List<OrderField<*>> {
		val fields = mutableListOf<OrderField<*>>(similarityScore.desc())
		sortFields.forEach { fields += if (it.direction == DESC) it.field.toJooqField().desc() else it.field.toJooqField().asc() }
		fields += TB_USER.LAST_NAME
		return fields
	}

	fun findAll(
		textSearched: String?,
		visibilitySearched: Boolean?,
		sortFields: List<SortModel<UserSortFieldEnum>> = emptyList(),
		limit: Int,
		offset: Int,
	): Flux<UserEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_USER.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			dsl.select(
				TB_USER.asterisk(),
				similarityScore,
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL,
				fullCount
			)
				.from(TB_USER)
				.leftJoin(creator).on(TB_USER.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_USER.LAST_MODIFIED_BY.eq(editor.ID))
				.where(notPurgedAndNotServiceAccount().and(searchCondition(textSearched)).and(visibleCondition(TB_USER.VISIBLE, visibilitySearched)))
				.orderBy(orderFields(similarityScore, sortFields))
				.limit(limit).offset(offset)
		).map { it.toEntity(creator, editor, fullCount) }
	}

	fun findWithLimit(textSearched: String?, visibilitySearched: Boolean?, limit: Int): Flux<UserEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			TB_USER.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
		return Flux.from(
			dsl.select(
				TB_USER.asterisk(),
				similarityScore,
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL
			)
				.from(TB_USER)
				.leftJoin(creator).on(TB_USER.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_USER.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					notPurgedAndNotServiceAccount().and(searchCondition(textSearched))
						.and(visibleCondition(TB_USER.VISIBLE, visibilitySearched))
				)
				.orderBy(similarityScore.desc(), TB_USER.LAST_NAME)
				.limit(limit)
		).map { it.toEntity(creator, editor) }
	}

	fun findById(id: UUID, visibilitySearched: Boolean?): Mono<UserEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			dsl.select(
				TB_USER.asterisk(),
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL
			)
				.from(TB_USER)
				.leftJoin(creator).on(TB_USER.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_USER.LAST_MODIFIED_BY.eq(editor.ID))
				.where(
					notPurgedAndNotServiceAccount().and(TB_USER.ID.eq(id))
						.and(visibleCondition(TB_USER.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(creator, editor) }
	}

	fun findServiceAccount(): Mono<CurrentUserEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			dsl.select(
				TB_USER.asterisk(),
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL
			)
				.from(TB_USER)
				.leftJoin(creator).on(TB_USER.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_USER.LAST_MODIFIED_BY.eq(editor.ID))
				.where(TB_USER.PURGED.isFalse.and(TB_USER.TYPE.eq(UserTypeEnum.SERVICE_ACCOUNT)))
		).map { it.toCurrentUserEntity(creator, editor, includesPreferences = false) }
	}

	fun findByOidcId(oidcId: UUID, visibilitySearched: Boolean?): Mono<CurrentUserEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			preferencesSelect(creator, editor)
				.where(
					TB_USER.TYPE.ne(UserTypeEnum.SERVICE_ACCOUNT).and(TB_USER.OIDC_ID.eq(oidcId))
						.and(visibleCondition(TB_USER.VISIBLE, visibilitySearched))
				)
		).map { it.toCurrentUserEntity(creator, editor, includesPreferences = true) }
	}

	fun findByEmail(email: String, visibilitySearched: Boolean?): Flux<CurrentUserEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			preferencesSelect(creator, editor)
				.where(
					TB_USER.TYPE.ne(UserTypeEnum.SERVICE_ACCOUNT).and(TB_USER.EMAIL.eq(email))
						.and(visibleCondition(TB_USER.VISIBLE, visibilitySearched))
				)
		).map { it.toCurrentUserEntity(creator, editor, includesPreferences = true) }
	}

	private fun preferencesSelect(creator: TbUser, editor: TbUser) =
		dsl.select(
			TB_USER.asterisk(),
			TB_PREFERENCES.ID,
			TB_PREFERENCES.THEME,
			TB_PREFERENCES.LANGUAGE,
			TB_PREFERENCES.SELECTED_PROFILE_ID,
			TB_PROJECT.ID,
			TB_PROJECT.NAME,
			TB_PROJECT.BEGIN_DATE,
			TB_PROJECT.BEGIN_TIME,
			TB_PROJECT.END_DATE,
			TB_PROJECT.END_TIME,
			TB_PROJECT.OPTIONS,
			TB_PROJECT_PROFILE.ROLE,
			TB_PROJECT_PROFILE.STATUS,
			TB_PROJECT_PROFILE.START_ACCESS_DATE,
			TB_PROJECT_PROFILE.START_ACCESS_TIME,
			TB_PROJECT_PROFILE.END_ACCESS_DATE,
			TB_PROJECT_PROFILE.END_ACCESS_TIME,
			creator.FIRST_NAME,
			creator.LAST_NAME,
			creator.EMAIL,
			editor.FIRST_NAME,
			editor.LAST_NAME,
			editor.EMAIL,
		)
			.from(TB_USER)
			.leftJoin(creator).on(TB_USER.CREATED_BY.eq(creator.ID))
			.leftJoin(editor).on(TB_USER.LAST_MODIFIED_BY.eq(editor.ID))
			.leftJoin(TB_PREFERENCES).on(TB_USER.ID.eq(TB_PREFERENCES.USER_ID).and(TB_PREFERENCES.VISIBLE.isTrue))
			.leftJoin(TB_PROJECT_PROFILE)
			.on(TB_PREFERENCES.SELECTED_PROFILE_ID.eq(TB_PROJECT_PROFILE.ID).and(TB_PROJECT_PROFILE.VISIBLE.isTrue))
			.leftJoin(TB_PROJECT).on(TB_PROJECT_PROFILE.PROJECT_ID.eq(TB_PROJECT.ID).and(TB_PROJECT.VISIBLE.isTrue))

	fun findByRoleLevel(roleLevel: Int, visibilitySearched: Boolean?): Flux<UserEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			dsl.select(
				TB_USER.asterisk(),
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL
			)
				.from(TB_USER)
				.leftJoin(creator).on(TB_USER.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_USER.LAST_MODIFIED_BY.eq(editor.ID))
				.join(TB_USER_ROLE).on(TB_USER.ROLE.eq(TB_USER_ROLE.NAME).and(TB_USER_ROLE.LEVEL.eq(roleLevel)))
				.where(notPurgedAndNotServiceAccount().and(visibleCondition(TB_USER.VISIBLE, visibilitySearched)))
		).map { it.toEntity(creator, editor) }
	}

	fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID> = Flux.from(
		dsl.select(TB_USER.ID)
			.from(TB_USER)
			.where(
				DSL.condition("{0}::date < {1}", TB_USER.LAST_LOGIN, DSL.`val`(dateThreshold))
					.and(TB_USER.TYPE.ne(UserTypeEnum.SERVICE_ACCOUNT))
			)
	).map { it.value1()!! }

	fun save(entity: UserEntity): Mono<UserEntity> = if (entity.id == null) insert(entity) else update(entity)

	private fun insert(entity: UserEntity): Mono<UserEntity> {
		val record = dsl.newRecord(TB_USER)
		record.setGeneric(entity, columns)
		entity.oidcId?.let { record.set(TB_USER.OIDC_ID, it) }
		entity.type?.let { record.set(TB_USER.TYPE, it) }
		entity.firstName?.let { record.set(TB_USER.FIRST_NAME, it) }
		entity.lastName?.let { record.set(TB_USER.LAST_NAME, it) }
		entity.email?.let { record.set(TB_USER.EMAIL, it) }
		entity.role?.let { record.set(TB_USER.ROLE, it) }
		entity.birthday?.let { record.set(TB_USER.BIRTHDAY, it) }
		entity.lastLogin?.let { record.set(TB_USER.LAST_LOGIN, it) }
		entity.purged?.let { record.set(TB_USER.PURGED, it) }
		return Mono.from(dsl.insertInto(TB_USER).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: UserEntity): Mono<UserEntity> = Mono.from(
		dsl.update(TB_USER)
			.setGeneric(entity, columns)
			.set(TB_USER.OIDC_ID, entity.oidcId)
			.set(TB_USER.TYPE, entity.type)
			.set(TB_USER.FIRST_NAME, entity.firstName)
			.set(TB_USER.LAST_NAME, entity.lastName)
			.set(TB_USER.EMAIL, entity.email)
			.set(TB_USER.ROLE, entity.role)
			.set(TB_USER.BIRTHDAY, entity.birthday)
			.set(TB_USER.LAST_LOGIN, entity.lastLogin)
			.set(TB_USER.PURGED, entity.purged)
			.where(TB_USER.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_USER).where(TB_USER.ID.eq(id))).map { }

	private fun Record.toEntity(
		creator: TbUser? = null,
		editor: TbUser? = null,
		fullCount: Field<Int>? = null
	): UserEntity =
		UserEntity(
			oidcId = get(TB_USER.OIDC_ID),
			type = get(TB_USER.TYPE),
			firstName = get(TB_USER.FIRST_NAME),
			lastName = get(TB_USER.LAST_NAME),
			email = get(TB_USER.EMAIL),
			role = get(TB_USER.ROLE),
			birthday = get(TB_USER.BIRTHDAY),
			lastLogin = get(TB_USER.LAST_LOGIN),
			purged = get(TB_USER.PURGED),
		).apply {
			fillGeneric(this, columns, creator, editor, fullCount)
		}

	private fun Record.toCurrentUserEntity(
		creator: TbUser? = null,
		editor: TbUser? = null,
		includesPreferences: Boolean,
	): CurrentUserEntity {
		return CurrentUserEntity(
			preferenceId = if (includesPreferences) get(TB_PREFERENCES.ID) else null,
			preferenceTheme = if (includesPreferences) get(TB_PREFERENCES.THEME) else null,
			preferenceLanguage = if (includesPreferences) get(TB_PREFERENCES.LANGUAGE) else null,
			preferenceSelectedProfileId = if (includesPreferences) get(TB_PREFERENCES.SELECTED_PROFILE_ID) else null,
			preferenceSelectedProfileRole = if (includesPreferences) get(TB_PROJECT_PROFILE.ROLE) else null,
			preferenceSelectedProfileStatus = if (includesPreferences) get(TB_PROJECT_PROFILE.STATUS) else null,
			preferenceSelectedProfileStartAccessDate = if (includesPreferences) get(TB_PROJECT_PROFILE.START_ACCESS_DATE) else null,
			preferenceSelectedProfileStartAccessTime = if (includesPreferences) get(TB_PROJECT_PROFILE.START_ACCESS_TIME) else null,
			preferenceSelectedProfileEndAccessDate = if (includesPreferences) get(TB_PROJECT_PROFILE.END_ACCESS_DATE) else null,
			preferenceSelectedProfileEndAccessTime = if (includesPreferences) get(TB_PROJECT_PROFILE.END_ACCESS_TIME) else null,
			preferenceSelectedProfileProjectId = if (includesPreferences) get(TB_PROJECT.ID) else null,
			preferenceSelectedProfileProjectName = if (includesPreferences) get(TB_PROJECT.NAME) else null,
			preferenceSelectedProfileProjectStartDate = if (includesPreferences) get(TB_PROJECT.BEGIN_DATE) else null,
			preferenceSelectedProfileProjectStartTime = if (includesPreferences) get(TB_PROJECT.BEGIN_TIME) else null,
			preferenceSelectedProfileProjectEndDate = if (includesPreferences) get(TB_PROJECT.END_DATE) else null,
			preferenceSelectedProfileProjectEndTime = if (includesPreferences) get(TB_PROJECT.END_TIME) else null,
			preferenceSelectedProfileProjectOptions = if (includesPreferences) get(TB_PROJECT.OPTIONS) else null,
		).apply {
			oidcId = get(TB_USER.OIDC_ID)
			type = get(TB_USER.TYPE)
			firstName = get(TB_USER.FIRST_NAME)
			lastName = get(TB_USER.LAST_NAME)
			email = get(TB_USER.EMAIL)
			role = get(TB_USER.ROLE)
			birthday = get(TB_USER.BIRTHDAY)
			lastLogin = get(TB_USER.LAST_LOGIN)
			purged = get(TB_USER.PURGED)
			fillGeneric(this, columns, creator, editor)
		}
	}
}
