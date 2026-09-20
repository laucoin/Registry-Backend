package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_PROFILE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.unaccent2
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.projectTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectField
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class ProjectProfileJooqRepository(private val dsl: DSLContext) {
	private fun userTable() = TB_USER.`as`("user_tb")

	private fun baseSelect(user: TbUser, project: TbProject, creator: TbUser, editor: TbUser, vararg extraFields: SelectField<*>): SelectOnConditionStep<Record> =
		dsl.select(
			listOf(
				TB_PROJECT_PROFILE.asterisk(),
				user.FIRST_NAME, user.LAST_NAME, user.EMAIL, user.LAST_LOGIN, user.PURGED, user.OIDC_ID,
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
				creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL,
			) + extraFields.toList()
		)
			.from(TB_PROJECT_PROFILE)
			.join(user).on(TB_PROJECT_PROFILE.USER_ID.eq(user.ID).and(user.VISIBLE.isTrue))
			.join(project).on(TB_PROJECT_PROFILE.PROJECT_ID.eq(project.ID).and(project.VISIBLE.isTrue))
			.leftJoin(creator).on(TB_PROJECT_PROFILE.CREATED_BY.eq(creator.ID))
			.leftJoin(editor).on(TB_PROJECT_PROFILE.LAST_MODIFIED_BY.eq(editor.ID))

	private fun textProjectSearchCondition(project: TbProject, textSearched: String?): Condition =
		textSearched?.let {
			val pattern = DSL.concat(DSL.inline("%"), unaccent2(DSL.`val`(it)), DSL.inline("%"))
			unaccent2(project.NAME).likeIgnoreCase(pattern)
		} ?: DSL.noCondition()

	private fun textUserSearchCondition(user: TbUser, textSearched: String?): Condition =
		textSearched?.let { similarity(user.SEARCH_TEXT, DSL.`val`(it)).gt(0f) } ?: DSL.noCondition()

	private fun usableCondition(availabilitySearched: Boolean?): Condition {
		if (availabilitySearched == null) return DSL.noCondition()
		val started = startedCondition(TB_PROJECT_PROFILE.START_ACCESS_DATE, TB_PROJECT_PROFILE.START_ACCESS_TIME)
		val notEnded = TB_PROJECT_PROFILE.END_ACCESS_DATE.isNull
			.or(TB_PROJECT_PROFILE.END_ACCESS_DATE.gt(DSL.currentLocalDate()))
			.or(TB_PROJECT_PROFILE.END_ACCESS_DATE.eq(DSL.currentLocalDate()).and(TB_PROJECT_PROFILE.END_ACCESS_TIME.isNull.or(TB_PROJECT_PROFILE.END_ACCESS_TIME.ge(DSL.currentOffsetTime()))))
		val isUsable = started.and(notEnded)
		return if (availabilitySearched) isUsable else isUsable.not()
	}

	private fun startedCondition(startDate: Field<LocalDate?>, startTime: Field<OffsetTime?>): Condition =
		startDate.isNull.or(startDate.lt(DSL.currentLocalDate()))
			.or(startDate.eq(DSL.currentLocalDate()).and(startTime.isNull.or(startTime.le(DSL.currentOffsetTime()))))

	private fun dateInRangeCondition(dateTimeSearched: ZonedDateTime?): Condition {
		if (dateTimeSearched == null) return DSL.noCondition()
		val date = dateTimeSearched.toLocalDate()
		val time = dateTimeSearched.toOffsetDateTime().toOffsetTime()
		val startsBefore = TB_PROJECT_PROFILE.START_ACCESS_DATE.isNull
			.or(TB_PROJECT_PROFILE.START_ACCESS_DATE.lt(date))
			.or(TB_PROJECT_PROFILE.START_ACCESS_DATE.eq(date).and(TB_PROJECT_PROFILE.START_ACCESS_TIME.isNull.or(TB_PROJECT_PROFILE.START_ACCESS_TIME.le(time))))
		val endsAfter = TB_PROJECT_PROFILE.END_ACCESS_DATE.isNull
			.or(TB_PROJECT_PROFILE.END_ACCESS_DATE.gt(date))
			.or(TB_PROJECT_PROFILE.END_ACCESS_DATE.eq(date).and(TB_PROJECT_PROFILE.END_ACCESS_TIME.isNull.or(TB_PROJECT_PROFILE.END_ACCESS_TIME.ge(time))))
		return startsBefore.and(endsAfter)
	}

	// Overlap (not "contains a point") check: is [startDateTimeSearched, endDateTimeSearched] within the
	// profile's own access window at all (used to detect a colliding existing profile), vs dateInRangeCondition
	// above which checks whether one instant falls inside the window.
	private fun datesOverlapCondition(startDateTimeSearched: ZonedDateTime?, endDateTimeSearched: ZonedDateTime?): Condition {
		val endsAfterSearchStart = startDateTimeSearched?.let {
			val date = it.toLocalDate()
			val time = it.toOffsetDateTime().toOffsetTime()
			TB_PROJECT_PROFILE.END_ACCESS_DATE.isNull
				.or(TB_PROJECT_PROFILE.END_ACCESS_DATE.gt(date))
				.or(TB_PROJECT_PROFILE.END_ACCESS_DATE.eq(date).and(TB_PROJECT_PROFILE.END_ACCESS_TIME.isNull.or(TB_PROJECT_PROFILE.END_ACCESS_TIME.gt(time))))
		} ?: DSL.noCondition()
		val startsBeforeSearchEnd = endDateTimeSearched?.let {
			val date = it.toLocalDate()
			val time = it.toOffsetDateTime().toOffsetTime()
			TB_PROJECT_PROFILE.START_ACCESS_DATE.isNull
				.or(TB_PROJECT_PROFILE.START_ACCESS_DATE.lt(date))
				.or(TB_PROJECT_PROFILE.START_ACCESS_DATE.eq(date).and(TB_PROJECT_PROFILE.START_ACCESS_TIME.isNull.or(TB_PROJECT_PROFILE.START_ACCESS_TIME.le(time))))
		} ?: DSL.noCondition()
		return endsAfterSearchStart.and(startsBeforeSearchEnd)
	}

	fun findByUserId(
		userId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(user, project, creator, editor, fullCount)
				.where(
					TB_PROJECT_PROFILE.USER_ID.eq(userId)
						.and(textProjectSearchCondition(project, textSearched))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(usableCondition(availabilitySearched))
						.and(TB_PROJECT_PROFILE.STATUS.`in`(statusSearched))
						.and(dateInRangeCondition(dateTimeSearched))
				)
				.orderBy(project.NAME)
				.limit(limit).offset(offset)
		).map { it.toEntity(user, project, creator, editor, fullCount) }
	}

	fun findByProjectId(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(user.SEARCH_TEXT, DSL.`val`(textSearched))).`as`("similarity_score")
		val fullCount = count().over().`as`("full_count")
		return Flux.from(
			baseSelect(user, project, creator, editor, similarityScore, fullCount)
				.where(
					TB_PROJECT_PROFILE.PROJECT_ID.eq(projectId)
						.and(textUserSearchCondition(user, textSearched))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(usableCondition(availabilitySearched))
						.and(TB_PROJECT_PROFILE.STATUS.`in`(statusSearched))
						.and(dateInRangeCondition(dateTimeSearched))
				)
				.orderBy(similarityScore.desc(), user.LAST_NAME)
				.limit(limit).offset(offset)
		).map { it.toEntity(user, project, creator, editor, fullCount) }
	}

	fun findUserIdsWithProjectProfileForProjectWithProfileExclusion(
		projectId: UUID,
		userIds: List<UUID>,
		profileIdToExclude: UUID?,
		statusSearched: List<ProfileStatusEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Flux<UUID> = Flux.from(
		dsl.selectDistinct(TB_PROJECT_PROFILE.USER_ID)
			.from(TB_PROJECT_PROFILE)
			.where(
				TB_PROJECT_PROFILE.PROJECT_ID.eq(projectId)
					.and(TB_PROJECT_PROFILE.USER_ID.`in`(userIds))
					.and(profileIdToExclude?.let { TB_PROJECT_PROFILE.ID.ne(it) } ?: DSL.noCondition())
					.and(TB_PROJECT_PROFILE.STATUS.`in`(statusSearched))
					.and(datesOverlapCondition(startDateTimeSearched, endDateTimeSearched))
			)
	).map { it.value1()!! }

	fun findAllRolesByUserId(
		userId: UUID,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
	): Flux<ProjectProfileRoleEntity> {
		val project = projectTable()
		return Flux.from(
			dsl.select(project.ID, project.OPTIONS, project.VISIBLE, TB_PROJECT_PROFILE.ROLE)
				.from(TB_PROJECT_PROFILE)
				.join(project).on(TB_PROJECT_PROFILE.PROJECT_ID.eq(project.ID))
				.where(
					TB_PROJECT_PROFILE.USER_ID.eq(userId)
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(usableCondition(availabilitySearched))
						.and(TB_PROJECT_PROFILE.STATUS.`in`(statusSearched))
				)
		).map { ProjectProfileRoleEntity(projectId = it.value1(), projectOptions = it.value2(), projectVisible = it.value3(), role = it.value4()) }
	}

	fun findOidcIdsByProjectId(projectId: UUID): Flux<UUID> = Flux.from(
		dsl.selectDistinct(TB_USER.OIDC_ID)
			.from(TB_PROJECT_PROFILE)
			.join(TB_USER).on(TB_PROJECT_PROFILE.USER_ID.eq(TB_USER.ID))
			.where(TB_PROJECT_PROFILE.PROJECT_ID.eq(projectId).and(TB_USER.OIDC_ID.isNotNull))
	).map { it.value1()!! }

	fun findProjectProfileByProjectAndUserId(
		projectId: UUID,
		userId: UUID,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
	): Mono<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(user, project, creator, editor)
				.where(
					TB_PROJECT_PROFILE.USER_ID.eq(userId)
						.and(TB_PROJECT_PROFILE.PROJECT_ID.eq(projectId))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(usableCondition(availabilitySearched))
						.and(TB_PROJECT_PROFILE.STATUS.`in`(statusSearched))
				)
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findLevel0ProjectProfileRoleByUserId(userId: UUID, visibilitySearched: Boolean?): Flux<ProjectProfileRoleCountEntity> {
		val userProfileProject = name("user_profile_project").`as`(
			dsl.select(TB_PROJECT_PROFILE.PROJECT_ID)
				.from(TB_PROJECT_PROFILE)
				.join(TB_PROJECT_ROLE).on(TB_PROJECT_PROFILE.ROLE.eq(TB_PROJECT_ROLE.NAME).and(TB_PROJECT_ROLE.LEVEL.eq(0)))
				.where(
					TB_PROJECT_PROFILE.STATUS.eq(ProfileStatusEnum.ACCEPTED)
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(startedCondition(TB_PROJECT_PROFILE.START_ACCESS_DATE, TB_PROJECT_PROFILE.START_ACCESS_TIME))
						.and(TB_PROJECT_PROFILE.END_ACCESS_DATE.isNull)
						.and(TB_PROJECT_PROFILE.END_ACCESS_TIME.isNull)
						.and(TB_PROJECT_PROFILE.USER_ID.eq(userId))
				)
		)
		val upProjectId = field(name("user_profile_project", "project_id"), UUID::class.java)
		val roleCount = count(TB_PROJECT_PROFILE.ROLE).`as`("role_count")
		return Flux.from(
			dsl.with(userProfileProject)
				.select(TB_PROJECT_PROFILE.PROJECT_ID, TB_PROJECT.NAME, roleCount)
				.from(TB_PROJECT_PROFILE)
				.join(TB_PROJECT_ROLE).on(TB_PROJECT_PROFILE.ROLE.eq(TB_PROJECT_ROLE.NAME).and(TB_PROJECT_ROLE.LEVEL.eq(0)))
				.join(TB_PROJECT).on(TB_PROJECT_PROFILE.PROJECT_ID.eq(TB_PROJECT.ID))
				.join(TB_USER).on(TB_PROJECT_PROFILE.USER_ID.eq(TB_USER.ID).and(TB_USER.PURGED.isFalse).and(visibleCondition(TB_USER.VISIBLE, visibilitySearched)))
				.join(userProfileProject).on(
					upProjectId.eq(TB_PROJECT.ID)
						.and(TB_PROJECT_PROFILE.STATUS.eq(ProfileStatusEnum.ACCEPTED))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(startedCondition(TB_PROJECT_PROFILE.START_ACCESS_DATE, TB_PROJECT_PROFILE.START_ACCESS_TIME))
						.and(TB_PROJECT_PROFILE.END_ACCESS_DATE.isNull)
						.and(TB_PROJECT_PROFILE.END_ACCESS_TIME.isNull)
				)
				.groupBy(TB_PROJECT_PROFILE.PROJECT_ID, TB_PROJECT.NAME)
		).map { ProjectProfileRoleCountEntity(projectId = it.value1(), projectName = it.value2(), level0 = it.value3()) }
	}

	fun findLevel0ProjectProfileRoleByProjectId(projectId: UUID, visibilitySearched: Boolean?): Flux<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(user, project, creator, editor)
				.join(TB_PROJECT_ROLE).on(TB_PROJECT_PROFILE.ROLE.eq(TB_PROJECT_ROLE.NAME))
				.where(
					visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched)
						.and(TB_PROJECT_ROLE.LEVEL.eq(0))
						.and(TB_PROJECT_PROFILE.PROJECT_ID.eq(projectId))
				)
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findByUserIdAndId(userId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(user, project, creator, editor)
				.where(TB_PROJECT_PROFILE.USER_ID.eq(userId).and(TB_PROJECT_PROFILE.ID.eq(id)).and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched)))
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findByProjectIdAndId(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(user, project, creator, editor)
				.where(TB_PROJECT_PROFILE.PROJECT_ID.eq(projectId).and(TB_PROJECT_PROFILE.ID.eq(id)).and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched)))
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun save(entity: ProjectProfileEntity): Mono<ProjectProfileEntity> = if (entity.id == null) insert(entity) else update(entity)

	fun saveAll(entities: List<ProjectProfileEntity>): Flux<ProjectProfileEntity> = Flux.fromIterable(entities).flatMap { save(it) }

	private fun insert(entity: ProjectProfileEntity): Mono<ProjectProfileEntity> {
		val record = dsl.newRecord(TB_PROJECT_PROFILE)
		entity.visible?.let { record.set(TB_PROJECT_PROFILE.VISIBLE, it) }
		entity.createdAt?.let { record.set(TB_PROJECT_PROFILE.CREATED_DATE, it) }
		entity.creatorId?.let { record.set(TB_PROJECT_PROFILE.CREATED_BY, it) }
		entity.lastUpdateAt?.let { record.set(TB_PROJECT_PROFILE.LAST_MODIFIED_DATE, it) }
		entity.lastEditorId?.let { record.set(TB_PROJECT_PROFILE.LAST_MODIFIED_BY, it) }
		entity.projectId?.let { record.set(TB_PROJECT_PROFILE.PROJECT_ID, it) }
		entity.userId?.let { record.set(TB_PROJECT_PROFILE.USER_ID, it) }
		entity.role?.let { record.set(TB_PROJECT_PROFILE.ROLE, it) }
		entity.status?.let { record.set(TB_PROJECT_PROFILE.STATUS, it) }
		entity.startAccessDate?.let { record.set(TB_PROJECT_PROFILE.START_ACCESS_DATE, it) }
		entity.startAccessTime?.let { record.set(TB_PROJECT_PROFILE.START_ACCESS_TIME, it) }
		entity.endAccessDate?.let { record.set(TB_PROJECT_PROFILE.END_ACCESS_DATE, it) }
		entity.endAccessTime?.let { record.set(TB_PROJECT_PROFILE.END_ACCESS_TIME, it) }
		return Mono.from(dsl.insertInto(TB_PROJECT_PROFILE).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: ProjectProfileEntity): Mono<ProjectProfileEntity> = Mono.from(
		dsl.update(TB_PROJECT_PROFILE)
			.set(TB_PROJECT_PROFILE.VISIBLE, entity.visible)
			.set(TB_PROJECT_PROFILE.CREATED_DATE, entity.createdAt)
			.set(TB_PROJECT_PROFILE.CREATED_BY, entity.creatorId)
			.set(TB_PROJECT_PROFILE.LAST_MODIFIED_DATE, entity.lastUpdateAt)
			.set(TB_PROJECT_PROFILE.LAST_MODIFIED_BY, entity.lastEditorId)
			.set(TB_PROJECT_PROFILE.PROJECT_ID, entity.projectId)
			.set(TB_PROJECT_PROFILE.USER_ID, entity.userId)
			.set(TB_PROJECT_PROFILE.ROLE, entity.role)
			.set(TB_PROJECT_PROFILE.STATUS, entity.status)
			.set(TB_PROJECT_PROFILE.START_ACCESS_DATE, entity.startAccessDate)
			.set(TB_PROJECT_PROFILE.START_ACCESS_TIME, entity.startAccessTime)
			.set(TB_PROJECT_PROFILE.END_ACCESS_DATE, entity.endAccessDate)
			.set(TB_PROJECT_PROFILE.END_ACCESS_TIME, entity.endAccessTime)
			.where(TB_PROJECT_PROFILE.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> = Mono.from(dsl.deleteFrom(TB_PROJECT_PROFILE).where(TB_PROJECT_PROFILE.ID.eq(id))).map { }

	private fun Record.toEntity(
		user: TbUser? = null,
		project: TbProject? = null,
		creator: TbUser? = null,
		editor: TbUser? = null,
		fullCount: Field<Int>? = null,
	): ProjectProfileEntity = ProjectProfileEntity(
		userId = get(TB_PROJECT_PROFILE.USER_ID),
		userFirstName = user?.let { get(it.FIRST_NAME) },
		userLastName = user?.let { get(it.LAST_NAME) },
		userEmail = user?.let { get(it.EMAIL) },
		userLastLogin = user?.let { get(it.LAST_LOGIN) },
		userPurged = user?.let { get(it.PURGED) },
		userOidcId = user?.let { get(it.OIDC_ID) },
		role = get(TB_PROJECT_PROFILE.ROLE),
		status = get(TB_PROJECT_PROFILE.STATUS),
		startAccessDate = get(TB_PROJECT_PROFILE.START_ACCESS_DATE),
		startAccessTime = get(TB_PROJECT_PROFILE.START_ACCESS_TIME),
		endAccessDate = get(TB_PROJECT_PROFILE.END_ACCESS_DATE),
		endAccessTime = get(TB_PROJECT_PROFILE.END_ACCESS_TIME),
	).apply {
		id = get(TB_PROJECT_PROFILE.ID)
		visible = get(TB_PROJECT_PROFILE.VISIBLE)
		createdAt = get(TB_PROJECT_PROFILE.CREATED_DATE)
		creatorId = get(TB_PROJECT_PROFILE.CREATED_BY)
		creatorFirstName = creator?.let { get(it.FIRST_NAME) }
		creatorLastName = creator?.let { get(it.LAST_NAME) }
		creatorEmail = creator?.let { get(it.EMAIL) }
		lastUpdateAt = get(TB_PROJECT_PROFILE.LAST_MODIFIED_DATE)
		lastEditorId = get(TB_PROJECT_PROFILE.LAST_MODIFIED_BY)
		lastEditorFirstName = editor?.let { get(it.FIRST_NAME) }
		lastEditorLastName = editor?.let { get(it.LAST_NAME) }
		lastEditorEmail = editor?.let { get(it.EMAIL) }
		this.fullCount = fullCount?.let { get(it)?.toLong() }
		projectId = get(TB_PROJECT_PROFILE.PROJECT_ID)
		projectName = project?.let { get(it.NAME) }
		projectStartDate = project?.let { get(it.BEGIN_DATE) }
		projectStartTime = project?.let { get(it.BEGIN_TIME) }
		projectEndDate = project?.let { get(it.END_DATE) }
		projectEndTime = project?.let { get(it.END_TIME) }
		projectOptions = project?.let { get(it.OPTIONS) }
	}
}
