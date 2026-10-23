package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_CRITICALITY
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_LAST_COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_LAST_COMMUNICATION_MESSAGE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_STATUS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertFields.ALERT_TITLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.ZonedDateTime
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(ALERT_TABLE)
data class AlertEntity(
	@Column(ALERT_DATE_TIME)
	var dateTime: ZonedDateTime? = null,
	@Column(ALERT_TITLE)
	var title: String? = null,
	@Column(ALERT_STATUS)
	var status: AlertStatusEnum? = null,
	@ReadOnlyProperty
	@Column(ALERT_CRITICALITY)
	var criticality: AlertCriticalityEnum? = null,
	@ReadOnlyProperty
	@Column(ALERT_LAST_COMMUNICATION_DATE_TIME)
	var lastCommunicationDateTime: ZonedDateTime? = null,
	@ReadOnlyProperty
	@Column(ALERT_LAST_COMMUNICATION_MESSAGE)
	var lastCommunicationMessage: String? = null,
): GenericProjectEntity()
