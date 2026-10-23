package fr.laucoin.registry.backend.infrastructure.driven.postgres

import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.test.TestContext
import io.r2dbc.spi.R2dbcException
import java.time.Duration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier

class RowLockContentionTest : TestContext() {
	@Autowired
	private lateinit var userPort: IUserPort

	@Autowired
	private lateinit var transactionalOperator: TransactionalOperator

	@Test
	fun `Should a second transaction fail with a lock timeout while the level 0 users are locked`() {
		// Arrange
		val locked = Sinks.one<Boolean>()
		val release = Sinks.empty<Void>()
		val holder = userPort.lockLevel0Users()
			.then(Mono.fromRunnable<Void> { locked.tryEmitValue(true) })
			.then(release.asMono())
			.`as`(transactionalOperator::transactional)
			.subscribe()
		locked.asMono().block(Duration.ofSeconds(10))

		// Act
		val waiting = userPort.lockLevel0Users().then().`as`(transactionalOperator::transactional)

		// Assert
		StepVerifier.create(waiting)
			.expectErrorMatches { error -> generateSequence(error) { it.cause }.any(::isLockTimeout) }
			.verify(Duration.ofSeconds(15))
		release.tryEmitEmpty()
		holder.dispose()
	}

	private fun isLockTimeout(error: Throwable) = error is R2dbcException && error.sqlState == "55P03"
}
