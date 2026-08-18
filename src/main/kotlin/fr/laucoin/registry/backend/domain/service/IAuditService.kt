package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import reactor.core.publisher.Mono

/**
 * The security audit trail. Wrap a privileged operation with
 * [audit]: on termination (success, error or cancellation) one append-only
 * entry is emitted with actor, action, target, outcome and the request
 * correlation id. Emission is best-effort: an audit failure never alters the
 * wrapped pipeline's result.
 */
interface IAuditService {
	fun <T : Any> audit(source: Mono<T>, actor: CurrentUserModel, action: AuditActionEnum, targetId: Any?): Mono<T>
}
