package fr.laucoin.registry.backend.config

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy

/**
 * Provides the reactive [DSLContext] jOOQ repositories query through, wrapping the R2DBC
 * [ConnectionFactory] in a [TransactionAwareConnectionFactoryProxy] so jOOQ statements join the
 * ambient R2DBC transaction (started via [TransactionalOperator]) instead of opening their own.
 */
@Configuration
class JooqConfig {
	@Bean
	fun dslContext(connectionFactory: ConnectionFactory): DSLContext =
		DSL.using(TransactionAwareConnectionFactoryProxy(connectionFactory), SQLDialect.POSTGRES)
}
