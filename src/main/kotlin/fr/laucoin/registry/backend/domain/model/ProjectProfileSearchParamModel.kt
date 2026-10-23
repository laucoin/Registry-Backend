package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import java.time.ZonedDateTime
import java.util.Objects

data class ProjectProfileSearchParamModel(
	var isVisible: Boolean? = null,
	var isAvailable: Boolean? = null,
	val status: List<ProfileStatusEnum> = ProfileStatusEnum.entries.toList(),
	var at: ZonedDateTime? = null,
	var role: String? = null,
	var sort: SortModel? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isAvailable: Boolean? = null,
		status: ProfileStatusEnum? = null,
		at: ZonedDateTime? = null,
		role: String? = null,
		sort: SortModel? = null,
	): this(
		if (status === BLOCKED) false else null,
		isAvailable,
		if (Objects.nonNull(status) && status!! != BLOCKED) listOf(status) else ProfileStatusEnum.entries.toList(),
		at,
		role?.takeUnless { it.isBlank() },
		sort = sort,
	) {
		this.query = if (query.isNullOrBlank()) null else query
	}

	companion object {
		val SORT_FIELDS = setOf(
			"firstName", "lastName", "email", "role", "status", "startAccess", "endAccess",
		)

		val PROJECT_SORT_FIELDS = setOf(
			"projectName", "role", "status", "startAccess", "endAccess",
		)
	}
}
