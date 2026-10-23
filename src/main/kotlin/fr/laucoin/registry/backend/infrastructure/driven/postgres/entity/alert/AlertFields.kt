package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert

object AlertFields {
	const val ALERT_TABLE = "tb_alert"

	const val ALERT_DATE_TIME = "date_time"
	const val ALERT_TITLE = "title"
	const val ALERT_STATUS = "status"
	const val ALERT_CRITICALITY = "criticality"
	const val ALERT_LAST_COMMUNICATION_DATE_TIME = "last_communication_date_time"
	const val ALERT_LAST_COMMUNICATION_MESSAGE = "last_communication_message"
}
