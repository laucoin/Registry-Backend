package fr.laucoin.registry.backend.domain.service.impl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.READ
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IAuditReadService
import fr.laucoin.registry.backend.domain.service.IAuditService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Objectif : Trace les lectures de données personnelles dans l'audit, sans ralentir la réponse.
 * Périmètre : Enregistre une ligne par fiche lue, et une seule ligne par fenêtre pour les lectures de liste d'un même acteur.
 * Limites : Déduplication locale à l'instance et remise à zéro au redémarrage ; ne trace pas le contenu lu.
 */
@Service
class AuditReadService(
	private val auditService: IAuditService,
	private val clock: Clock,
	@Value($$"${registry.audit.read-dedup-window}")
	window: Duration,
) : IAuditReadService {
	private val recentListReads: Cache<ListReadKey, Boolean> = Caffeine.newBuilder()
		.expireAfterWrite(window)
		.maximumSize(MAX_TRACKED_LIST_READS)
		.ticker(Ticker { Duration.between(Instant.EPOCH, clock.instant()).toNanos() })
		.build()

	override fun recordRead(
		currentUser: CurrentUserModel,
		resourceType: AuditResourceTypeEnum,
		resourceId: UUID?,
		projectId: UUID?,
		client: ClientContextModel,
		outcome: AuditOutcomeEnum,
	) {
		if (resourceId == null && isRecentListRead(ListReadKey(currentUser.id, resourceType, projectId, outcome))) return

		auditService.record(
			AuditModel(READ, resourceType, resourceId, projectId, currentUser.id, outcome, client, ZonedDateTime.now(clock)),
		)
	}

	private fun isRecentListRead(key: ListReadKey): Boolean = recentListReads.asMap().putIfAbsent(key, true) != null

	private data class ListReadKey(
		val actorUserId: UUID?,
		val resourceType: AuditResourceTypeEnum,
		val projectId: UUID?,
		val outcome: AuditOutcomeEnum,
	)

	private companion object {
		const val MAX_TRACKED_LIST_READS = 10_000L
	}
}
