package fr.laucoin.registry.backend.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.binder.MeterBinder
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.PoolMetrics
import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionalOperator

/**
 * Objectif : Configure la transaction réactive R2DBC et les métriques du pool de connexions.
 * Périmètre : Expose le gestionnaire de transactions, le `TransactionalOperator` et les jauges du pool.
 * Limites : Ne définit ni la connexion ni le dimensionnement du pool, portés par `application.yml`.
 */
@Configuration
@EnableTransactionManagement
class R2dbcConfig {
	@Bean
	fun transactionManager(connection: ConnectionFactory): R2dbcTransactionManager {
		return R2dbcTransactionManager(connection)
	}

	@Bean
	fun transactionalOperator(manager: ReactiveTransactionManager): TransactionalOperator {
		return TransactionalOperator.create(manager)
	}

	@Bean
	fun r2dbcPoolMetricsBinder(connectionFactory: ConnectionFactory): MeterBinder {
		return MeterBinder { registerPoolGauges(connectionFactory, it) }
	}

	private fun registerPoolGauges(connectionFactory: ConnectionFactory, registry: MeterRegistry) {
		val metrics = (connectionFactory as? ConnectionPool)?.metrics?.orElse(null) ?: return
		gauge("r2dbc.pool.acquired", "Connections currently acquired from the pool", metrics, registry) { it.acquiredSize() }
		gauge(
			"r2dbc.pool.allocated",
			"Connections currently allocated by the pool (acquired + idle)",
			metrics,
			registry,
		) { it.allocatedSize() }
		gauge("r2dbc.pool.idle", "Connections currently idle in the pool", metrics, registry) { it.idleSize() }
		gauge(
			"r2dbc.pool.pending",
			"Pending connection acquisitions waiting on the pool",
			metrics,
			registry,
		) { it.pendingAcquireSize() }
	}

	private fun gauge(
		name: String,
		description: String,
		metrics: PoolMetrics,
		registry: MeterRegistry,
		size: (PoolMetrics) -> Int,
	) {
		Gauge.builder(name, metrics) { size(it).toDouble() }.description(description).register(registry)
	}
}
