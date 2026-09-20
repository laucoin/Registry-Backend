package fr.laucoin.registry.backend.infrastructure.driven.postgres.converter

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import org.jooq.Converter

class ProjectOptionArrayConverter : Converter<Array<String>, List<ProjectOptionEnum>> {
	override fun from(databaseObject: Array<String>?): List<ProjectOptionEnum>? =
		databaseObject?.map { ProjectOptionEnum.valueOf(it) }

	override fun to(userObject: List<ProjectOptionEnum>?): Array<String>? =
		userObject?.map { it.name }?.toTypedArray()

	override fun fromType(): Class<Array<String>> = Array<String>::class.java

	@Suppress("UNCHECKED_CAST")
	override fun toType(): Class<List<ProjectOptionEnum>> = List::class.java as Class<List<ProjectOptionEnum>>
}
