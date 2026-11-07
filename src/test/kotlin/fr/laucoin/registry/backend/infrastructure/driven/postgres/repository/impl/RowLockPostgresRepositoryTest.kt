package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.test.StepVerifier

class RowLockPostgresRepositoryTest : TestContext() {
	@Autowired
	private lateinit var port: IRowLockPort

	@Autowired
	private lateinit var transactionalOperator: TransactionalOperator

	@ParameterizedTest
	@EnumSource(LockableResourceEnum::class)
	fun `Should lock complete for a missing row of every lockable resource`(resource: LockableResourceEnum) {
		// Act
		val result = port.lock(resource, UUID.randomUUID()).`as`(transactionalOperator::transactional)

		// Assert
		StepVerifier.create(result).expectNext(Unit).verifyComplete()
	}
}
