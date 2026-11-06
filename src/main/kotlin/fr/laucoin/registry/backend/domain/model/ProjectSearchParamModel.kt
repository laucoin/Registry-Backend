package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.annotation.Default
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectSearchParamModel(
	var isVisible: Boolean? = null,
	var at: ZonedDateTime? = null,
	var sort: SortModel? = null,
) {
	var query: String? = null
	var isFavorite: Boolean? = null
	var userId: UUID? = null
	var hiddenReadableIds: List<UUID> = emptyList()

	@Default
	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		at: ZonedDateTime? = null,
		sort: SortModel? = null,
		isFavorite: Boolean? = null,
	): this(isVisible, at, sort = sort) {
		this.isFavorite = isFavorite
		this.query = if (query.isNullOrBlank()) null else query
	}

	/** Copie des filtres pour l'utilisateur qui consulte : identifiant pour les favoris, projets masqués qu'il peut lire. */
	fun forViewer(viewerId: UUID?, readableHiddenIds: List<UUID>): ProjectSearchParamModel =
		ProjectSearchParamModel(query, isVisible, at, sort, isFavorite).apply {
			userId = viewerId
			hiddenReadableIds = readableHiddenIds
		}

	companion object {
		val SORT_FIELDS = setOf(
			"name", "begin", "end",
			"participants", "groups", "acceptedProfiles", "vehicles", "activities", "activeAlerts",
			"favorite",
		)
	}
}
