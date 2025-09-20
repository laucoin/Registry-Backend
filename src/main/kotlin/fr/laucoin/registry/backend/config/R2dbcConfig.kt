package fr.laucoin.registry.backend.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionalOperator

@Configuration
@EnableTransactionManagement
@EnableR2dbcAuditing
class R2dbcConfig {
	@Bean
	fun transactionManager(connection: ConnectionFactory): R2dbcTransactionManager {
		return R2dbcTransactionManager(connection)
	}

	@Bean
	fun transactionalOperator(manager: ReactiveTransactionManager): TransactionalOperator {
		return TransactionalOperator.create(manager)
	}
}
