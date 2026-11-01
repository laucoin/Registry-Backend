package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.util.UUID

class HistoryModel(
	var dateTime: ZonedDateTime,
	var user: HistoryUserModel? = null
) {
	data class HistoryUserModel(
		var id: UUID? = null,
		var firstName: String? = null,
		var lastName: String? = null,
		var email: String? = null,
	)
}
