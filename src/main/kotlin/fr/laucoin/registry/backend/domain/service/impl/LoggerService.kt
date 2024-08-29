package fr.laucoin.registry.backend.domain.service.impl

import org.slf4j.LoggerFactory

open class LoggerService {
    protected val log: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)
}
