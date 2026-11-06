package fr.laucoin.registry.backend.infrastructure.driving.security.service

import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.service.LoggerService
import java.net.InetAddress
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component

/**
 * Objectif : Résout le contexte client d'une requête (IP réelle et User-Agent) pour la traçabilité.
 * Périmètre : Lit X-Forwarded-For uniquement lorsque l'appelant direct est un proxy de confiance, en remontant la chaîne de droite à gauche jusqu'au premier saut non fiable.
 * Limites : Ne résout jamais de nom d'hôte (littéraux IP seulement) ; sans proxy configuré, seule l'adresse de la connexion fait foi, ce qu'il signale au démarrage.
 */
@Component
class ClientContextService(
	@Value($$"${registry.security.trusted-proxies:}")
	trustedProxies: List<String>,
) : LoggerService() {
	private val trusted = trustedProxies.map(String::trim).filter(String::isNotEmpty).map(::IpAddressMatcher)

	init {
		if (trusted.isEmpty()) log.info("No trusted proxy is configured: the client IP is the connection address")
	}

	fun resolve(request: ServerHttpRequest): ClientContextModel {
		val peer = request.remoteAddress?.address?.hostAddress
		val userAgent = request.headers.getFirst(USER_AGENT)
		return ClientContextModel(ip = clientIp(peer, request), userAgent = userAgent)
	}

	private fun clientIp(peer: String?, request: ServerHttpRequest): String? {
		if (peer == null || !isTrusted(peer)) return peer
		val chain = request.headers[FORWARDED_FOR].orEmpty().flatMap { it.split(',') }.map(String::trim)
		return chain.asReversed().firstOrNull { isIpLiteral(it) && !isTrusted(it) } ?: chain.firstOrNull(::isIpLiteral) ?: peer
	}

	private fun isTrusted(ip: String): Boolean = trusted.any { it.matches(ip) }

	private fun isIpLiteral(value: String): Boolean {
		return (IPV4.matches(value) || (':' in value && IPV6.matches(value))) && runCatching { InetAddress.getByName(value) }.isSuccess
	}

	private companion object {
		const val USER_AGENT = "User-Agent"
		const val FORWARDED_FOR = "X-Forwarded-For"
		val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
		val IPV6 = Regex("^[0-9a-fA-F:.]+$")
	}
}
