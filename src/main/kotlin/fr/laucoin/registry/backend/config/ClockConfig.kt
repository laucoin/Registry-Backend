package fr.laucoin.registry.backend.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Purpose: Exposes the application clock as a bean.
 * Scope: Provides the single UTC `Clock` that services and queries use for the current instant.
 * Limits: Holds no time zone logic; callers convert as needed.
 */
@Configuration
class ClockConfig {
	@Bean
	fun clock(): Clock = Clock.systemUTC()
}
