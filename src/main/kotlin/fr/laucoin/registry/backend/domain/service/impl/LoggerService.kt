package fr.laucoin.registry.backend.domain.service.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class LoggerService {
	protected val log: Logger = LoggerFactory.getLogger(this::class.java)
}
