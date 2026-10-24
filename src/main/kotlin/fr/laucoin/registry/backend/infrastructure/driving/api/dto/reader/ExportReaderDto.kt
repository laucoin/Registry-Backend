package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

/** Modification trace; the author is only flagged, never identified, to protect third parties. */
@JsonInclude(NON_NULL)
data class ExportHistoryReaderDto(
	val dateTime: ZonedDateTime?,
	val byDataSubject: Boolean,
)

@JsonInclude(NON_NULL)
data class ExportMovementReaderDto(
	val dateTime: ZonedDateTime,
	val type: String?,
	val reason: String?,
	val activity: String?,
)

@JsonInclude(NON_NULL)
data class ExportPreferencesReaderDto(
	val theme: String,
	val language: String?,
)

@JsonInclude(NON_NULL)
data class ExportProjectProfileReaderDto(
	val projectId: UUID?,
	val projectName: String?,
	val role: String?,
	val status: String?,
	val startAccess: CustomDateTimeModel?,
	val endAccess: CustomDateTimeModel?,
)

@JsonInclude(NON_NULL)
data class ExportParticipantReaderDto(
	val id: UUID?,
	val projectId: UUID?,
	val projectName: String?,
	val firstName: String?,
	val lastName: String?,
	val birthday: LocalDate?,
	val type: String?,
	val startAvailability: CustomDateTimeModel?,
	val endAvailability: CustomDateTimeModel?,
	val groups: List<String>,
	val movements: List<ExportMovementReaderDto>,
	val creation: ExportHistoryReaderDto?,
	val lastEdition: ExportHistoryReaderDto?,
)

@JsonInclude(NON_NULL)
data class ExportUserDataReaderDto(
	val id: UUID?,
	val oidcId: UUID?,
	val type: String,
	val firstName: String?,
	val lastName: String?,
	val email: String?,
	val role: String?,
	val birthday: LocalDate?,
	val lastLogin: ZonedDateTime?,
	val blocked: Boolean,
)

@JsonInclude(NON_NULL)
data class ParticipantExportReaderDto(
	val schemaVersion: String = "1.0",
	val generatedAt: ZonedDateTime,
	val subjectType: String = "PARTICIPANT",
	val participant: ExportParticipantReaderDto,
)

@JsonInclude(NON_NULL)
data class UserExportReaderDto(
	val schemaVersion: String = "1.0",
	val generatedAt: ZonedDateTime,
	val subjectType: String = "USER",
	val user: ExportUserDataReaderDto,
	val preferences: ExportPreferencesReaderDto?,
	val projectProfiles: List<ExportProjectProfileReaderDto>,
	val participants: List<ExportParticipantReaderDto>,
)
