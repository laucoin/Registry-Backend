package fr.laucoin.registry.backend.config

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
		return MeterBinder { registry ->
			val metrics = (connectionFactory as? ConnectionPool)?.metrics?.orElse(null) ?: return@MeterBinder
			Gauge.builder("r2dbc.pool.acquired", metrics) { it.acquiredSize().toDouble() }
				.description("Connections currently acquired from the pool")
				.register(registry)
			Gauge.builder("r2dbc.pool.allocated", metrics) { it.allocatedSize().toDouble() }
				.description("Connections currently allocated by the pool (acquired + idle)")
				.register(registry)
			Gauge.builder("r2dbc.pool.idle", metrics) { it.idleSize().toDouble() }
				.description("Connections currently idle in the pool")
				.register(registry)
			Gauge.builder("r2dbc.pool.pending", metrics) { it.pendingAcquireSize().toDouble() }
				.description("Pending connection acquisitions waiting on the pool")
				.register(registry)
		}
	}
}
