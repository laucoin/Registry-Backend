package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.TbUser
import fr.laucoin.registry.backend.infrastructure.driven.postgres.jooq.tables.references.TB_PREFERENCES
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.creatorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.editorTable
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericJooqQueries.visibleCondition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class PreferencesJooqRepository(private val dsl: DSLContext) {
	fun findByUserId(userId: UUID, visibilitySearched: Boolean?): Mono<PreferencesEntity> {
		val creator = creatorTable()
		val editor = editorTable()
		return Mono.from(
			dsl.select(TB_PREFERENCES.asterisk(), creator.FIRST_NAME, creator.LAST_NAME, creator.EMAIL, editor.FIRST_NAME, editor.LAST_NAME, editor.EMAIL)
				.from(TB_PREFERENCES)
				.leftJoin(creator).on(TB_PREFERENCES.CREATED_BY.eq(creator.ID))
				.leftJoin(editor).on(TB_PREFERENCES.LAST_MODIFIED_BY.eq(editor.ID))
				.where(TB_PREFERENCES.USER_ID.eq(userId))
				.and(visibleCondition(TB_PREFERENCES.VISIBLE, visibilitySearched))
		).map { it.toEntity(creator, editor) }
	}

	fun save(entity: PreferencesEntity): Mono<PreferencesEntity> = if (entity.id == null) insert(entity) else update(entity)

	// INSERT skips null-valued fields (letting DB defaults apply), matching Spring Data R2DBC's insert semantics.
	private fun insert(entity: PreferencesEntity): Mono<PreferencesEntity> {
		val record = dsl.newRecord(TB_PREFERENCES)
		entity.visible?.let { record.set(TB_PREFERENCES.VISIBLE, it) }
		entity.createdAt?.let { record.set(TB_PREFERENCES.CREATED_DATE, it) }
		entity.creatorId?.let { record.set(TB_PREFERENCES.CREATED_BY, it) }
		entity.lastUpdateAt?.let { record.set(TB_PREFERENCES.LAST_MODIFIED_DATE, it) }
		entity.lastEditorId?.let { record.set(TB_PREFERENCES.LAST_MODIFIED_BY, it) }
		entity.userId?.let { record.set(TB_PREFERENCES.USER_ID, it) }
		record.set(TB_PREFERENCES.THEME, entity.theme)
		entity.language?.let { record.set(TB_PREFERENCES.LANGUAGE, it) }
		entity.selectedProfileId?.let { record.set(TB_PREFERENCES.SELECTED_PROFILE_ID, it) }
		return Mono.from(dsl.insertInto(TB_PREFERENCES).set(record).returning()).map { it.toEntity() }
	}

	// UPDATE writes every writable field unconditionally (nulls included), matching Spring Data R2DBC's full-row update.
	private fun update(entity: PreferencesEntity): Mono<PreferencesEntity> = Mono.from(
		dsl.update(TB_PREFERENCES)
			.set(TB_PREFERENCES.VISIBLE, entity.visible)
			.set(TB_PREFERENCES.CREATED_DATE, entity.createdAt)
			.set(TB_PREFERENCES.CREATED_BY, entity.creatorId)
			.set(TB_PREFERENCES.LAST_MODIFIED_DATE, entity.lastUpdateAt)
			.set(TB_PREFERENCES.LAST_MODIFIED_BY, entity.lastEditorId)
			.set(TB_PREFERENCES.USER_ID, entity.userId)
			.set(TB_PREFERENCES.THEME, entity.theme)
			.set(TB_PREFERENCES.LANGUAGE, entity.language)
			.set(TB_PREFERENCES.SELECTED_PROFILE_ID, entity.selectedProfileId)
			.where(TB_PREFERENCES.ID.eq(entity.id))
			.returning()
	).map { it.toEntity() }

	private fun Record.toEntity(creator: TbUser? = null, editor: TbUser? = null): PreferencesEntity =
		PreferencesEntity(
			userId = get(TB_PREFERENCES.USER_ID),
			theme = get(TB_PREFERENCES.THEME) ?: ThemeEnum.SYSTEM,
			language = get(TB_PREFERENCES.LANGUAGE),
			selectedProfileId = get(TB_PREFERENCES.SELECTED_PROFILE_ID),
		).apply {
			id = get(TB_PREFERENCES.ID)
			visible = get(TB_PREFERENCES.VISIBLE)
			createdAt = get(TB_PREFERENCES.CREATED_DATE)
			creatorId = get(TB_PREFERENCES.CREATED_BY)
			creatorFirstName = creator?.let { get(it.FIRST_NAME) }
			creatorLastName = creator?.let { get(it.LAST_NAME) }
			creatorEmail = creator?.let { get(it.EMAIL) }
			lastUpdateAt = get(TB_PREFERENCES.LAST_MODIFIED_DATE)
			lastEditorId = get(TB_PREFERENCES.LAST_MODIFIED_BY)
			lastEditorFirstName = editor?.let { get(it.FIRST_NAME) }
			lastEditorLastName = editor?.let { get(it.LAST_NAME) }
			lastEditorEmail = editor?.let { get(it.EMAIL) }
		}
}
