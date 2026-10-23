package fr.laucoin.registry.backend.infrastructure.driven.postgres.extension

import java.time.Clock
import java.time.ZonedDateTime
import org.springframework.data.spel.spi.EvaluationContextExtension
import org.springframework.stereotype.Component

/**
 * Purpose: Exposes the application clock to the SpEL expressions of the repository queries.
 * Scope: Gives the queries the current instant through `:#{registryClock.now}` instead of the database clock.
 * Limits: Provides the instant only; the time zone handling stays in the queries.
 */
@Component
class ClockEvaluationContextExtension(private val clock: Clock): EvaluationContextExtension {
	override fun getExtensionId(): String = "registryClock"

	override fun getRootObject(): Any = ClockRoot(clock)

	class ClockRoot(private val clock: Clock) {
		val now: ZonedDateTime get() = ZonedDateTime.now(clock)
	}
}
