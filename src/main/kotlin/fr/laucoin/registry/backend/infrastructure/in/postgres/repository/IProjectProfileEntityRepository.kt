package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.VISIBLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_END_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_ROLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_START_ACCESS_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_STATUS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.PROJECT_PROFILE_USER_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileFields.ROLE_COUNT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.DATES_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.JOIN_USER
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.LINKED_USER_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.PROJECT_PROFILE_FAVORITE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.PROJECT_PROFILE_STATUS_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.PROJECT_PROFILE_TEXT_PROJECT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.PROJECT_PROFILE_TEXT_USER_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.PROJECT_PROFILE_USABLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.SELECT_PROJECT_PROFILE_USER_SEARCH
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileQueries.SENT_INVITATION_CLAUSE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileRoleEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.role.RoleFields.ENTITY_ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.role.RoleFields.PROJECT_ROLE_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.LINKED_PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

@Repository
interface IProjectProfileEntityRepository : ReactiveCrudRepository<ProjectProfileEntity, UUID> {
	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$PROJECT_PROFILE_USER_ID = :userId AND $PROJECT_PROFILE_TEXT_PROJECT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PROJECT_PROFILE_USABLE_CLAUSE AND $PROJECT_PROFILE_STATUS_CLAUSE AND $DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE AND $PROJECT_PROFILE_FAVORITE_CLAUSE
        ORDER BY $LINKED_PROJECT_TABLE.$PROJECT_NAME
        LIMIT :limit OFFSET :offset
        """
	)
	fun findByUserId(
		userId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
		favoriteSearched: Boolean?,
		limit: Int,
		offset: Int,
	): Flux<ProjectProfileEntity>

	@Query(
		"""
        SELECT COUNT(t.$ID)
        FROM $PROJECT_PROFILE_TABLE t $PROJECT_JOIN
        WHERE t.$PROJECT_PROFILE_USER_ID = :userId AND $PROJECT_PROFILE_TEXT_PROJECT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PROJECT_PROFILE_USABLE_CLAUSE AND $PROJECT_PROFILE_STATUS_CLAUSE AND $DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE AND $PROJECT_PROFILE_FAVORITE_CLAUSE
        """
	)
	fun countByUserId(
		userId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
		favoriteSearched: Boolean?,
	): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $SENT_INVITATION_CLAUSE
        ORDER BY t.last_modified_date DESC
        LIMIT :limit OFFSET :offset
        """
	)
	fun findSentInvitationsByCreatorId(
		creatorId: UUID,
		since: ZonedDateTime,
		limit: Int,
		offset: Int,
	): Flux<ProjectProfileEntity>

	@Query(
		"""
        SELECT COUNT(t.$ID)
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN
        WHERE $SENT_INVITATION_CLAUSE
        """
	)
	fun countSentInvitationsByCreatorId(creatorId: UUID, since: ZonedDateTime): Mono<Long>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_PROJECT_PROFILE_USER_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$LINKED_PROJECT_ID = :projectId AND $PROJECT_PROFILE_TEXT_USER_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PROJECT_PROFILE_USABLE_CLAUSE AND $PROJECT_PROFILE_STATUS_CLAUSE AND $DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, $LINKED_USER_TABLE.$USER_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
	)
	fun findByProjectId(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
		limit: Int,
		offset: Int,
	): Flux<ProjectProfileEntity>

	@Query(
		"""
        SELECT COUNT(t.$ID)
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN
        WHERE t.$LINKED_PROJECT_ID = :projectId AND $PROJECT_PROFILE_TEXT_USER_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PROJECT_PROFILE_USABLE_CLAUSE AND $PROJECT_PROFILE_STATUS_CLAUSE AND $DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE
        """
	)
	fun countByProjectId(
		projectId: UUID,
		textSearched: String?,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
		dateTimeSearched: ZonedDateTime?,
	): Mono<Long>

	@Query(
		"""
        SELECT DISTINCT t.$PROJECT_PROFILE_USER_ID
        FROM $PROJECT_PROFILE_TABLE t
        WHERE t.$LINKED_PROJECT_ID = :projectId AND t.$PROJECT_PROFILE_USER_ID IN (:userIds) AND (:profileIdToExclude IS NULL OR t.$ID != :profileIdToExclude) AND $PROJECT_PROFILE_STATUS_CLAUSE AND $DATES_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE
        """
	)
	fun findUserIdsWithProjectProfileForProjectWithProfileExclusion(
		projectId: UUID,
		userIds: List<UUID>,
		profileIdToExclude: UUID?,
		statusSearched: List<ProfileStatusEnum>,
		startDateTimeSearched: ZonedDateTime?,
		endDateTimeSearched: ZonedDateTime?,
	): Flux<UUID>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_PROJECT
        FROM $PROJECT_PROFILE_TABLE t INNER JOIN $PROJECT_TABLE $LINKED_PROJECT_TABLE ON t.$LINKED_PROJECT_ID = $LINKED_PROJECT_TABLE.$ID
        WHERE t.$PROJECT_PROFILE_USER_ID = :userId AND $VISIBLE_CLAUSE AND $PROJECT_PROFILE_USABLE_CLAUSE AND $PROJECT_PROFILE_STATUS_CLAUSE
        """
	)
	fun findAllRolesByUserId(
		userId: UUID,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
	): Flux<ProjectProfileRoleEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER
        INNER JOIN $PROJECT_TABLE $LINKED_PROJECT_TABLE ON t.$LINKED_PROJECT_ID = $LINKED_PROJECT_TABLE.$ID
        $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$PROJECT_PROFILE_USER_ID = :userId AND $LINKED_PROJECT_ID = :projectId AND $VISIBLE_CLAUSE AND $PROJECT_PROFILE_USABLE_CLAUSE AND $PROJECT_PROFILE_STATUS_CLAUSE
        """
	)
	fun findProjectProfileByProjectAndUserId(
		projectId: UUID,
		userId: UUID,
		visibilitySearched: Boolean?,
		availabilitySearched: Boolean?,
		statusSearched: List<ProfileStatusEnum>,
	): Mono<ProjectProfileEntity>

	@Query(
		"""
        WITH user_profile_project AS (
            SELECT t.$LINKED_PROJECT_ID
            FROM $PROJECT_PROFILE_TABLE t
            INNER JOIN $PROJECT_ROLE_TABLE tpr ON t.$PROJECT_PROFILE_ROLE = tpr.$ENTITY_ROLE_NAME AND tpr.$ROLE_LEVEL = 0
            WHERE t.$PROJECT_PROFILE_STATUS = 'ACCEPTED'
            AND (:visibilitySearched IS NULL OR t.$VISIBLE = :visibilitySearched)
            AND (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) < CURRENT_DATE OR (COALESCE(t.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(t.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME))
            AND t.$PROJECT_PROFILE_END_ACCESS_DATE IS NULL AND t.$PROJECT_PROFILE_END_ACCESS_TIME IS NULL
            AND t.$PROJECT_PROFILE_USER_ID = :userId
        )
        SELECT tpp.$LINKED_PROJECT_ID, tp.$PROJECT_NAME AS $LINKED_PROJECT_NAME, COUNT(tpp.$PROJECT_PROFILE_ROLE) AS $ROLE_COUNT
        FROM $PROJECT_PROFILE_TABLE tpp
        INNER JOIN $PROJECT_ROLE_TABLE tpr ON tpp.$PROJECT_PROFILE_ROLE = tpr.$ENTITY_ROLE_NAME AND tpr.$ROLE_LEVEL = 0
        INNER JOIN $PROJECT_TABLE tp ON tpp.$LINKED_PROJECT_ID = tp.$ID
        INNER JOIN $USER_TABLE tu ON tpp.$PROJECT_PROFILE_USER_ID = tu.$ID AND (:visibilitySearched IS NULL OR tu.$VISIBLE = :visibilitySearched)
        INNER JOIN user_profile_project up ON up.$LINKED_PROJECT_ID = tp.$ID
        AND tpp.$PROJECT_PROFILE_STATUS = 'ACCEPTED' AND (:visibilitySearched IS NULL OR tpp.$VISIBLE = :visibilitySearched) AND (COALESCE(tpp.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) < CURRENT_DATE OR (COALESCE(tpp.$PROJECT_PROFILE_START_ACCESS_DATE, '-infinity'::DATE) = CURRENT_DATE AND COALESCE(tpp.$PROJECT_PROFILE_START_ACCESS_TIME, '00:00:00.000000'::TIME) <= CURRENT_TIME))
        AND tpp.$PROJECT_PROFILE_END_ACCESS_DATE IS NULL AND tpp.$PROJECT_PROFILE_END_ACCESS_TIME IS NULL
        GROUP BY tpp.$LINKED_PROJECT_ID, tp.$PROJECT_NAME
        """
	)
	fun findLevel0ProjectProfileRoleByUserId(
		userId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileRoleCountEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $PROJECT_ROLE_TABLE ON t.$PROJECT_PROFILE_ROLE = $PROJECT_ROLE_TABLE.$ENTITY_ROLE_NAME
        WHERE $VISIBLE_CLAUSE
        AND $PROJECT_ROLE_TABLE.$ROLE_LEVEL = 0
        AND t.$LINKED_PROJECT_ID = :projectId
        """
	)
	fun findLevel0ProjectProfileRoleByProjectId(
		projectId: UUID,
		visibilitySearched: Boolean?
	): Flux<ProjectProfileEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$PROJECT_PROFILE_USER_ID = :userId AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findByUserIdAndId(
		userId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
	): Mono<ProjectProfileEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PROJECT_PROFILE_TABLE t $JOIN_USER $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$LINKED_PROJECT_ID = :projectId AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findByProjectIdAndId(
		projectId: UUID,
		id: UUID,
		visibilitySearched: Boolean?,
	): Mono<ProjectProfileEntity>
}
