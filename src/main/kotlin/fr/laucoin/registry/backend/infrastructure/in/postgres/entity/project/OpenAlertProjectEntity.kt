package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_NAME
import org.springframework.data.relational.core.mapping.Column
import java.util.UUID

/**
 * Projection for the open-alert-projects dashboard query (a project id/name +
 * its open-alert count). Not a table — mapped from the aggregate SELECT.
 */
data class OpenAlertProjectEntity(
	@Column(LINKED_PROJECT_ID)
	var projectId: UUID? = null,
	@Column(LINKED_PROJECT_NAME)
	var projectName: String? = null,
	@Column(OPEN_ALERT_COUNT)
	var openAlertCount: Long = 0,
) {
	companion object {
		const val OPEN_ALERT_COUNT = "open_alert_count"
	}
}
