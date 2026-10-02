package fr.laucoin.registry.backend.domain.service.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Trivial base class providing a subclass-scoped SLF4J [Logger] (`log`) so services don't each wire
 * their own. Carries no other behavior.
 */
open class LoggerService {
	protected val log: Logger = LoggerFactory.getLogger(this::class.java)
}
