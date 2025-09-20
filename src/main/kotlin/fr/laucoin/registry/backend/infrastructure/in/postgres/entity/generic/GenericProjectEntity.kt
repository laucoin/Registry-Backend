package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_START_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_START_TIME
import java.time.LocalDate
import java.time.OffsetTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

abstract class GenericProjectEntity(
	@Column(LINKED_PROJECT_ID)
	var projectId: UUID? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_NAME)
	var projectName: String? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_START_DATE)
	var projectStartDate: LocalDate? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_START_TIME)
	var projectStartTime: OffsetTime? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_END_DATE)
	var projectEndDate: LocalDate? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_END_TIME)
	var projectEndTime: OffsetTime? = null,
	@ReadOnlyProperty
	@Column(LINKED_PROJECT_OPTIONS)
	var projectOptions: List<ProjectOptionEnum>? = null,
): GenericEntity()
