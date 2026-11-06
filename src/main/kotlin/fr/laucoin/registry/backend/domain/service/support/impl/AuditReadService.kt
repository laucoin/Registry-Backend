package fr.laucoin.registry.backend.domain.service.support.impl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.READ
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.support.IAuditReadService
import fr.laucoin.registry.backend.domain.service.support.IAuditService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Objectif : Trace les lectures de données personnelles dans l'audit, sans ralentir la réponse.
 * Périmètre : Enregistre une ligne par fiche lue ; une seule ligne par fenêtre pour les lectures de liste d'un même acteur, et pour ses lectures refusées, tous chemins confondus.
 * Limites : Déduplication locale à l'instance et remise à zéro au redémarrage ; ne trace pas le contenu lu.
 */
@Service
class AuditReadService(
	private val auditService: IAuditService,
	private val clock: Clock,
	@Value($$"${registry.audit.read-dedup-window}")
	window: Duration,
) : IAuditReadService {
	private val recentReads: Cache<ReadKey, Boolean> = Caffeine.newBuilder()
		.expireAfterWrite(window)
		.maximumSize(MAX_TRACKED_READS)
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
		if (isRecentRead(readKey(currentUser, resourceType, resourceId, projectId, outcome))) return

		auditService.record(
			AuditModel(READ, resourceType, resourceId, projectId, currentUser.id, outcome, client, ZonedDateTime.now(clock)),
		)
	}

	private fun readKey(
		currentUser: CurrentUserModel,
		resourceType: AuditResourceTypeEnum,
		resourceId: UUID?,
		projectId: UUID?,
		outcome: AuditOutcomeEnum,
	): ReadKey? = when {
		outcome != AuditOutcomeEnum.SUCCESS -> ReadKey(currentUser.id, resourceType, null, outcome)
		resourceId == null -> ReadKey(currentUser.id, resourceType, projectId, outcome)
		else -> null
	}

	private fun isRecentRead(key: ReadKey?): Boolean = key != null && recentReads.asMap().putIfAbsent(key, true) != null

	private data class ReadKey(
		val actorUserId: UUID?,
		val resourceType: AuditResourceTypeEnum,
		val projectId: UUID?,
		val outcome: AuditOutcomeEnum,
	)

	private companion object {
		const val MAX_TRACKED_READS = 10_000L
	}
}
