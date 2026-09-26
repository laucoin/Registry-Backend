package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum.CREATED_DATE
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum.LAST_MODIFIED_DATE
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum.USER_FIRST_NAME
import fr.laucoin.registry.backend.domain.enumeration.ProjectProfileSortFieldEnum.USER_LAST_NAME
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum.DESC
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.similarity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.routines.references.unaccent2
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ACTIVITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_ALERT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_GROUP
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_PROFILE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PROJECT_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_USER
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_VEHICLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.GenericColumns
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.activeAtCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.activeNowCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.fillGenericProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.notEndedCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.projectTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGeneric
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.setGenericProject
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.notStartedCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.startedCondition
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.OrderField
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
import java.time.ZonedDateTime
import java.util.UUID

/**
 * jOOQ queries against `TB_PROJECT_PROFILE`: CRUD (single and bulk), similarity-ranked/filtered/
 * sorted/paginated search scoped to a User or a Project, the invitation-conflict lookup, and the
 * level-0-administrator queries backing the last-administrator safeguard. Consumed by
 * [ProjectProfileModelPostgresRepository], never by the domain directly.
 */
@Repository
class ProjectProfileJooqRepository(private val dsl: DSLContext) {
	private val columns = GenericColumns(
		TB_PROJECT_PROFILE.ID,
		TB_PROJECT_PROFILE.VISIBLE,
		TB_PROJECT_PROFILE.CREATED_DATE,
		TB_PROJECT_PROFILE.CREATED_BY,
		TB_PROJECT_PROFILE.LAST_MODIFIED_DATE,
		TB_PROJECT_PROFILE.LAST_MODIFIED_BY
	)

	private fun userTable() = TB_USER.`as`("user_tb")

	/**
	 * Scalar subqueries counting, for one Project row, its visible participants/vehicles/groups/
	 * activities/accepted-profiles and IN_PROGRESS alerts — correlated on [project]'s id so they ride
	 * along in the same query as the Project(Profile) row instead of a separate round trip per count.
	 */
	private class ProjectCountsFields(
		val participants: Field<Int>,
		val vehicles: Field<Int>,
		val groups: Field<Int>,
		val activities: Field<Int>,
		val profiles: Field<Int>,
		val ongoingAlerts: Field<Int>,
	) {
		fun toSelectFields(): Array<SelectField<*>> = arrayOf(participants, vehicles, groups, activities, profiles, ongoingAlerts)
	}

	// The gated counts (vehicles/activities/ongoingAlerts) fold the "is this option enabled" check into the
	// subquery's WHERE rather than a CASE expression: if the option isn't in project.options, the WHERE
	// matches no row and COUNT(*) naturally comes back 0, mirroring the pre-SQL decorateWithCounts behavior.
	private fun hasOptionCondition(project: TbProject, option: ProjectOptionEnum): Condition =
		DSL.condition("{0} @> ARRAY[{1}]::text[]", project.OPTIONS, DSL.inline(option.name))

	// Postgres can't reference a SELECT-list alias from WHERE/ORDER BY, so callers needing the ongoing-alerts
	// count in a condition (e.g. findAcceptedByUserId) must build their own instance via this function rather
	// than reuse ProjectCountsFields.ongoingAlerts — jOOQ re-embeds the full subquery at each usage site.
	private fun ongoingAlertsCountField(project: TbProject): Field<Int> = DSL.field(
		dsl.select(count(TB_ALERT.ID)).from(TB_ALERT)
			.where(
				TB_ALERT.PROJECT_ID.eq(project.ID)
					.and(TB_ALERT.VISIBLE.isTrue)
					.and(TB_ALERT.STATUS.eq(IN_PROGRESS))
					.and(hasOptionCondition(project, ProjectOptionEnum.ALERT))
			)
	)

	private fun projectCountsFields(project: TbProject): ProjectCountsFields {
		val countProfile = TB_PROJECT_PROFILE.`as`("counts_profile_tb")
		val countUser = TB_USER.`as`("counts_profile_user_tb")
		return ProjectCountsFields(
			participants = DSL.field(
				dsl.select(count(TB_PARTICIPANT.ID)).from(TB_PARTICIPANT)
					.where(TB_PARTICIPANT.PROJECT_ID.eq(project.ID).and(TB_PARTICIPANT.PURGED.isFalse).and(TB_PARTICIPANT.VISIBLE.isTrue))
			).`as`("participants_count"),
			vehicles = DSL.field(
				dsl.select(count(TB_VEHICLE.ID)).from(TB_VEHICLE)
					.where(
						TB_VEHICLE.PROJECT_ID.eq(project.ID)
							.and(TB_VEHICLE.VISIBLE.isTrue)
							.and(hasOptionCondition(project, ProjectOptionEnum.VEHICLE))
					)
			).`as`("vehicles_count"),
			groups = DSL.field(
				dsl.select(count(TB_GROUP.ID)).from(TB_GROUP)
					.where(TB_GROUP.PROJECT_ID.eq(project.ID).and(TB_GROUP.VISIBLE.isTrue))
			).`as`("groups_count"),
			activities = DSL.field(
				dsl.select(count(TB_ACTIVITY.ID)).from(TB_ACTIVITY)
					.where(
						TB_ACTIVITY.PROJECT_ID.eq(project.ID)
							.and(TB_ACTIVITY.VISIBLE.isTrue)
							.and(hasOptionCondition(project, ProjectOptionEnum.ACTIVITY))
					)
			).`as`("activities_count"),
			profiles = DSL.field(
				dsl.select(count(countProfile.ID)).from(countProfile)
					.join(countUser).on(countProfile.USER_ID.eq(countUser.ID).and(countUser.VISIBLE.isTrue))
					.where(countProfile.PROJECT_ID.eq(project.ID).and(countProfile.VISIBLE.isTrue).and(countProfile.STATUS.eq(ACCEPTED)))
			).`as`("profiles_count"),
			ongoingAlerts = ongoingAlertsCountField(project).`as`("ongoing_alerts_count"),
		)
	}

	private fun baseSelect(
		user: TbUser,
		project: TbProject,
		creator: TbUser,
		editor: TbUser,
		vararg extraFields: SelectField<*>
	): SelectOnConditionStep<Record> =
		dsl.select(
			listOf(
				TB_PROJECT_PROFILE.asterisk(),
				user.FIRST_NAME,
				user.LAST_NAME,
				user.EMAIL,
				user.LAST_LOGIN,
				user.PURGED,
				user.OIDC_ID,
				creator.FIRST_NAME,
				creator.LAST_NAME,
				creator.EMAIL,
				editor.FIRST_NAME,
				editor.LAST_NAME,
				editor.EMAIL,
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
		val isUsable = activeNowCondition(
			TB_PROJECT_PROFILE.START_ACCESS_DATE,
			TB_PROJECT_PROFILE.START_ACCESS_TIME,
			TB_PROJECT_PROFILE.END_ACCESS_DATE,
			TB_PROJECT_PROFILE.END_ACCESS_TIME
		)
		return if (availabilitySearched) isUsable else isUsable.not()
	}

	private fun upcomingCondition(project: TbProject, upcomingSearched: Boolean?): Condition {
		if (upcomingSearched == null) return DSL.noCondition()
		val isUpcoming = notStartedCondition(project.BEGIN_DATE, project.BEGIN_TIME)
		return if (upcomingSearched) isUpcoming else isUpcoming.not()
	}

	private fun dateInRangeCondition(dateTimeSearched: ZonedDateTime?): Condition =
		dateTimeSearched?.let {
			activeAtCondition(
				TB_PROJECT_PROFILE.START_ACCESS_DATE,
				TB_PROJECT_PROFILE.START_ACCESS_TIME,
				TB_PROJECT_PROFILE.END_ACCESS_DATE,
				TB_PROJECT_PROFILE.END_ACCESS_TIME,
				it
			)
		} ?: DSL.noCondition()

	private fun datesOverlapCondition(
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Condition {
		val endsAfterSearchStart = startDateTimeSearched?.let {
			val date = it.toLocalDate()
			val time = it.toOffsetDateTime().toOffsetTime()
			TB_PROJECT_PROFILE.END_ACCESS_DATE.isNull
				.or(TB_PROJECT_PROFILE.END_ACCESS_DATE.gt(date))
				.or(
					TB_PROJECT_PROFILE.END_ACCESS_DATE.eq(date)
						.and(TB_PROJECT_PROFILE.END_ACCESS_TIME.isNull.or(TB_PROJECT_PROFILE.END_ACCESS_TIME.gt(time)))
				)
		} ?: DSL.noCondition()
		val startsBeforeSearchEnd = endDateTimeSearched?.let {
			val date = it.toLocalDate()
			val time = it.toOffsetDateTime().toOffsetTime()
			TB_PROJECT_PROFILE.START_ACCESS_DATE.isNull
				.or(TB_PROJECT_PROFILE.START_ACCESS_DATE.lt(date))
				.or(
					TB_PROJECT_PROFILE.START_ACCESS_DATE.eq(date)
						.and(TB_PROJECT_PROFILE.START_ACCESS_TIME.isNull.or(TB_PROJECT_PROFILE.START_ACCESS_TIME.le(time)))
				)
		} ?: DSL.noCondition()
		return endsAfterSearchStart.and(startsBeforeSearchEnd)
	}

	private fun ProjectProfileSortFieldEnum.toJooqField(user: TbUser): Field<*> = when (this) {
		USER_LAST_NAME -> user.LAST_NAME
		USER_FIRST_NAME -> user.FIRST_NAME
		CREATED_DATE -> TB_PROJECT_PROFILE.CREATED_DATE
		LAST_MODIFIED_DATE -> TB_PROJECT_PROFILE.LAST_MODIFIED_DATE
	}

	// tiebreaker stays last as the final, stable order (v1's sole, implicit order).
	private fun orderFields(
		user: TbUser,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>>,
		tiebreaker: OrderField<*>,
		vararg leading: OrderField<*>,
	): List<OrderField<*>> {
		val fields = mutableListOf<OrderField<*>>(*leading)
		sortFields.forEach { fields += if (it.direction == DESC) it.field.toJooqField(user).desc() else it.field.toJooqField(user).asc() }
		fields += tiebreaker
		return fields
	}

	fun findAllByCreatorId(userId: UUID): Flux<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(user, project, creator, editor)
				.where(TB_PROJECT_PROFILE.CREATED_BY.eq(userId))
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findByUserId(
		userId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
		favoriteSearched: Boolean?,
		upcomingSearched: Boolean?,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>> = emptyList(),
		limit: Int,
		offset: Int,
		includeCounts: Boolean = false,
	): Flux<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val fullCount = count().over().`as`("full_count")
		val counts = if (includeCounts) projectCountsFields(project) else null
		return Flux.from(
			baseSelect(
				user, project, creator, editor, fullCount,
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
				*(counts?.toSelectFields() ?: emptyArray()),
			)
				.where(
					TB_PROJECT_PROFILE.USER_ID.eq(userId)
						.and(textProjectSearchCondition(project, textSearched))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(usableCondition(availabilitySearched))
						.and(TB_PROJECT_PROFILE.STATUS.`in`(statusSearched))
						.and(dateInRangeCondition(dateTimeSearched))
						.and(visibleCondition(TB_PROJECT_PROFILE.FAVORITE, favoriteSearched))
						.and(upcomingCondition(project, upcomingSearched))
				)
				.orderBy(orderFields(user, sortFields, tiebreaker = project.NAME))
				.limit(limit).offset(offset)
		).map { it.toEntity(user, project, creator, editor, fullCount, counts, includeProjectDetails = true) }
	}

	fun findAcceptedByUserId(userId: UUID, limit: Int): Flux<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val counts = projectCountsFields(project)
		val ongoingAlertsFilter = ongoingAlertsCountField(project)
		return Flux.from(
			baseSelect(
				user, project, creator, editor,
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
				*counts.toSelectFields(),
			)
				.where(
					TB_PROJECT_PROFILE.USER_ID.eq(userId)
						.and(TB_PROJECT_PROFILE.VISIBLE.isTrue)
						.and(TB_PROJECT_PROFILE.STATUS.eq(ProfileStatusEnum.ACCEPTED))
						.and(notEndedCondition(project.END_DATE, project.END_TIME))
						.and(ongoingAlertsFilter.gt(0))
				)
				.orderBy(ongoingAlertsFilter.desc(), project.NAME.asc())
				.limit(limit)
		).map { it.toEntity(user, project, creator, editor, counts = counts, includeProjectDetails = true) }
	}

	fun findByProjectId(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
		sortFields: List<SortModel<ProjectProfileSortFieldEnum>> = emptyList(),
		limit: Int,
		offset: Int,
	): Flux<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		val similarityScore = (if (textSearched == null) DSL.inline(1f) else similarity(
			user.SEARCH_TEXT,
			DSL.`val`(textSearched)
		)).`as`("similarity_score")
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
				.orderBy(orderFields(user, sortFields, tiebreaker = user.LAST_NAME, leading = arrayOf(similarityScore.desc())))
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
		).map {
			ProjectProfileRoleEntity(
				projectId = it.value1(),
				projectOptions = it.value2(),
				projectVisible = it.value3(),
				role = it.value4()
			)
		}
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

	fun findProjectProfilesByProjectIdsAndUserId(
		projectIds: List<UUID>,
		userId: UUID,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
	): Flux<ProjectProfileEntity> {
		if (projectIds.isEmpty()) return Flux.empty()
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Flux.from(
			baseSelect(user, project, creator, editor)
				.where(
					TB_PROJECT_PROFILE.USER_ID.eq(userId)
						.and(TB_PROJECT_PROFILE.PROJECT_ID.`in`(projectIds))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(usableCondition(availabilitySearched))
						.and(TB_PROJECT_PROFILE.STATUS.`in`(statusSearched))
				)
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun findLevel0ProjectProfileRoleByUserId(
		userId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileRoleCountEntity> {
		val userProfileProject = name("user_profile_project").`as`(
			dsl.select(TB_PROJECT_PROFILE.PROJECT_ID)
				.from(TB_PROJECT_PROFILE)
				.join(TB_PROJECT_ROLE)
				.on(TB_PROJECT_PROFILE.ROLE.eq(TB_PROJECT_ROLE.NAME).and(TB_PROJECT_ROLE.LEVEL.eq(0)))
				.where(
					TB_PROJECT_PROFILE.STATUS.eq(ProfileStatusEnum.ACCEPTED)
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(
							startedCondition(
								TB_PROJECT_PROFILE.START_ACCESS_DATE,
								TB_PROJECT_PROFILE.START_ACCESS_TIME
							)
						)
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
				.join(TB_PROJECT_ROLE)
				.on(TB_PROJECT_PROFILE.ROLE.eq(TB_PROJECT_ROLE.NAME).and(TB_PROJECT_ROLE.LEVEL.eq(0)))
				.join(TB_PROJECT).on(TB_PROJECT_PROFILE.PROJECT_ID.eq(TB_PROJECT.ID))
				.join(TB_USER).on(
					TB_PROJECT_PROFILE.USER_ID.eq(TB_USER.ID).and(TB_USER.PURGED.isFalse)
						.and(visibleCondition(TB_USER.VISIBLE, visibilitySearched))
				)
				.join(userProfileProject).on(
					upProjectId.eq(TB_PROJECT.ID)
						.and(TB_PROJECT_PROFILE.STATUS.eq(ProfileStatusEnum.ACCEPTED))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
						.and(
							startedCondition(
								TB_PROJECT_PROFILE.START_ACCESS_DATE,
								TB_PROJECT_PROFILE.START_ACCESS_TIME
							)
						)
						.and(TB_PROJECT_PROFILE.END_ACCESS_DATE.isNull)
						.and(TB_PROJECT_PROFILE.END_ACCESS_TIME.isNull)
				)
				.groupBy(TB_PROJECT_PROFILE.PROJECT_ID, TB_PROJECT.NAME)
		).map {
			ProjectProfileRoleCountEntity(
				projectId = it.value1(),
				projectName = it.value2(),
				level0 = it.value3()
			)
		}
	}

	fun findLevel0ProjectProfileRoleByProjectId(
		projectId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileEntity> {
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
			baseSelect(
				user, project, creator, editor,
				project.NAME, project.BEGIN_DATE, project.BEGIN_TIME, project.END_DATE, project.END_TIME, project.OPTIONS,
			)
				.where(
					TB_PROJECT_PROFILE.USER_ID.eq(userId).and(TB_PROJECT_PROFILE.ID.eq(id))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(user, project, creator, editor, includeProjectDetails = true) }
	}

	fun findByProjectIdAndId(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ProjectProfileEntity> {
		val user = userTable()
		val project = projectTable()
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			baseSelect(user, project, creator, editor)
				.where(
					TB_PROJECT_PROFILE.PROJECT_ID.eq(projectId).and(TB_PROJECT_PROFILE.ID.eq(id))
						.and(visibleCondition(TB_PROJECT_PROFILE.VISIBLE, visibilitySearched))
				)
		).map { it.toEntity(user, project, creator, editor) }
	}

	fun save(entity: ProjectProfileEntity): Mono<ProjectProfileEntity> =
		if (entity.id == null) insert(entity) else update(entity)

	fun saveAll(entities: List<ProjectProfileEntity>): Flux<ProjectProfileEntity> =
		Flux.fromIterable(entities).flatMap { save(it) }

	private fun insert(entity: ProjectProfileEntity): Mono<ProjectProfileEntity> {
		val record = dsl.newRecord(TB_PROJECT_PROFILE)
		record.setGeneric(entity, columns)
		record.setGenericProject(entity, TB_PROJECT_PROFILE.PROJECT_ID)
		entity.userId?.let { record.set(TB_PROJECT_PROFILE.USER_ID, it) }
		entity.role?.let { record.set(TB_PROJECT_PROFILE.ROLE, it) }
		entity.status?.let { record.set(TB_PROJECT_PROFILE.STATUS, it) }
		entity.startAccessDate?.let { record.set(TB_PROJECT_PROFILE.START_ACCESS_DATE, it) }
		entity.startAccessTime?.let { record.set(TB_PROJECT_PROFILE.START_ACCESS_TIME, it) }
		entity.endAccessDate?.let { record.set(TB_PROJECT_PROFILE.END_ACCESS_DATE, it) }
		entity.endAccessTime?.let { record.set(TB_PROJECT_PROFILE.END_ACCESS_TIME, it) }
		entity.favorite?.let { record.set(TB_PROJECT_PROFILE.FAVORITE, it) }
		return Mono.from(dsl.insertInto(TB_PROJECT_PROFILE).set(record).returning()).map { it.toEntity() }
	}

	private fun update(entity: ProjectProfileEntity): Mono<ProjectProfileEntity> = Mono.from(
		dsl.update(TB_PROJECT_PROFILE)
			.setGeneric(entity, columns)
			.setGenericProject(entity, TB_PROJECT_PROFILE.PROJECT_ID)
			.set(TB_PROJECT_PROFILE.USER_ID, entity.userId)
			.set(TB_PROJECT_PROFILE.ROLE, entity.role)
			.set(TB_PROJECT_PROFILE.STATUS, entity.status)
			.set(TB_PROJECT_PROFILE.START_ACCESS_DATE, entity.startAccessDate)
			.set(TB_PROJECT_PROFILE.START_ACCESS_TIME, entity.startAccessTime)
			.set(TB_PROJECT_PROFILE.END_ACCESS_DATE, entity.endAccessDate)
			.set(TB_PROJECT_PROFILE.END_ACCESS_TIME, entity.endAccessTime)
			.set(TB_PROJECT_PROFILE.FAVORITE, entity.favorite)
			.where(TB_PROJECT_PROFILE.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	fun deleteById(id: UUID): Mono<Unit> =
		Mono.from(dsl.deleteFrom(TB_PROJECT_PROFILE).where(TB_PROJECT_PROFILE.ID.eq(id))).map { }

	private fun Record.toEntity(
		user: TbUser? = null,
		project: TbProject? = null,
		creator: TbUser? = null,
		editor: TbUser? = null,
		fullCount: Field<Int>? = null,
		counts: ProjectCountsFields? = null,
		includeProjectDetails: Boolean = false,
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
		favorite = get(TB_PROJECT_PROFILE.FAVORITE),
		participantsCount = counts?.let { get(it.participants) },
		vehiclesCount = counts?.let { get(it.vehicles) },
		groupsCount = counts?.let { get(it.groups) },
		activitiesCount = counts?.let { get(it.activities) },
		profilesCount = counts?.let { get(it.profiles) },
		ongoingAlertsCount = counts?.let { get(it.ongoingAlerts) },
	).apply {
		fillGeneric(this, columns, creator, editor, fullCount)
		fillGenericProject(this, TB_PROJECT_PROFILE.PROJECT_ID, if (includeProjectDetails) project else null)
	}
}
