package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IProjectPort
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Assemble les éléments d'export d'un participant et parcourt les résultats paginés complets.
 * Périmètre : Charge le projet et les mouvements d'un participant, et enchaîne les pages d'un port jusqu'à épuisement.
 * Limites : Ne contrôle aucune autorisation et n'écrit aucun audit ; l'appelant décide de ce qui peut être exporté.
 */
@Component
class ExportAssemblyHelper(
	private val participantPort: IParticipantPort,
	private val movementPort: IMovementPort,
	private val projectPort: IProjectPort,
) {
	private companion object {
		private const val EXPORT_PAGE_SIZE = 200
	}

	fun findAllParticipantExports(userId: UUID, generatedAt: ZonedDateTime) =
		participantPort.findAllByUserId(userId)
			.concatMap { participantPort.findById(it.projectId!!, it.id!!, isVisible = null) }
			.concatMap { buildParticipantExport(it, generatedAt) }

	fun buildParticipantExport(
		participant: ParticipantModel,
		generatedAt: ZonedDateTime
	): Mono<ParticipantExportModel> {
		val projectId = participant.projectId!!

		return Mono.zip(
			projectPort.findById(projectId, isVisible = null).map { it.name ?: "" }.defaultIfEmpty(""),
			findAllMovements(projectId, participant.id!!).collectList(),
		).map { ParticipantExportModel(generatedAt, it.t1.ifEmpty { null }, participant, it.t2) }
	}

	private fun findAllMovements(projectId: UUID, participantId: UUID): Flux<MovementModel> =
		allPages {
			movementPort.findPageByParticipantId(projectId, participantId, it, MovementSearchParamModel())
		}

	fun <T : Any> allPages(pageNumber: Int = 0, fetch: (PageableModel) -> Mono<PageModel<T>>): Flux<T> =
		fetch(PageableModel(pageNumber.toLong() * EXPORT_PAGE_SIZE, EXPORT_PAGE_SIZE))
			.flatMapMany { page -> Flux.fromIterable(page.content).concatWith(nextPages(page, pageNumber, fetch)) }

	private fun <T : Any> nextPages(
		page: PageModel<T>,
		pageNumber: Int,
		fetch: (PageableModel) -> Mono<PageModel<T>>,
	): Flux<T> = if (pageNumber + 1 < page.totalPages) allPages(pageNumber + 1, fetch) else Flux.empty()
}
