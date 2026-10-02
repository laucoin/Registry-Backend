package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import java.time.ZonedDateTime
import java.util.Objects

data class ProjectProfileSearchParamModel(
	var isVisible: Boolean? = null,
	var isAvailable: Boolean? = null,
	val status: List<ProfileStatusEnum> = ProfileStatusEnum.entries.toList(),
	var dateTime: ZonedDateTime? = null,
	var isFavorite: Boolean? = null,
	var isUpcoming: Boolean? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isAvailable: Boolean? = null,
		status: ProfileStatusEnum? = null,
		dateTime: ZonedDateTime? = null,
		isFavorite: Boolean? = null,
		isUpcoming: Boolean? = null,
	): this(
		if (status === BLOCKED) false else null,
		isAvailable,
		if (Objects.nonNull(status) && status!! != BLOCKED) listOf(status) else ProfileStatusEnum.entries.toList(),
		dateTime,
		isFavorite,
		isUpcoming,
	) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}
