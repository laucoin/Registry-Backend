package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_STATUS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.alert.AlertFields.ALERT_TITLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericProjectEntity
import java.time.ZonedDateTime
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
): GenericProjectEntity()
