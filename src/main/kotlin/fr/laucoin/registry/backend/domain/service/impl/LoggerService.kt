package fr.laucoin.registry.backend.domain.service.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Objectif : Classe de base fournissant un logger aux services.
 * Périmètre : Expose un logger nommé d'après la classe concrète.
 * Limites : Ne porte aucun cas d'usage.
 */
open class LoggerService {
	protected val log: Logger = LoggerFactory.getLogger(this::class.java)
}
