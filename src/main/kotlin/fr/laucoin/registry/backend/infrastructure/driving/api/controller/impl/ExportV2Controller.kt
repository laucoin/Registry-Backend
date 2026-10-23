package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IExportService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IExportV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ExportReaderDtoMapper
import java.util.UUID
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Purpose: REST adapter of the personal data export endpoints.
 * Scope: Maps export models to DTOs and sets the download headers.
 * Limits: Holds no business rule; delegates to IExportService.
 */
@RestController
class ExportV2Controller(
	private val service: IExportService,
	private val mapper: ExportReaderDtoMapper,
): IExportV2Controller {
	override fun exportCurrentUser(currentUser: CurrentUserModel): Mono<ResponseEntity<UserExportReaderDto>> {
		return exportUserById(currentUser, currentUser.id!!)
	}

	override fun exportUserById(currentUser: CurrentUserModel, id: UUID): Mono<ResponseEntity<UserExportReaderDto>> {
		return service.exportUser(currentUser, id).map(mapper::toDto).map { attachment(it, "user-$id.json") }
	}

	override fun exportParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ResponseEntity<ParticipantExportReaderDto>> {
		return service.exportParticipant(currentUser, projectId, id).map(mapper::toDto)
			.map { attachment(it, "participant-$id.json") }
	}

	private fun <T : Any> attachment(body: T, filename: String): ResponseEntity<T> {
		return ResponseEntity.ok()
			.contentType(APPLICATION_JSON)
			.header(CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
			.header(CACHE_CONTROL, "no-store")
			.body(body)
	}
}
