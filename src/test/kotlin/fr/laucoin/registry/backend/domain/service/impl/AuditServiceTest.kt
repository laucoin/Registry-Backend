package fr.laucoin.registry.backend.domain.service.impl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fr.laucoin.registry.backend.domain.constant.AuditConst.CORRELATION_ID_CONTEXT_KEY
import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.USER_DELETE
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.test.ModelExt.userId
import fr.laucoin.registry.backend.test.ModelExt.userOidcId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.CONFLICT
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.util.context.Context
import java.time.Duration

class AuditServiceTest {
	private val service = AuditService()
	private val actor = CurrentUserModel(UserModel().apply { id = userId; oidcId = userOidcId })
	private val auditLogger = LoggerFactory.getLogger("fr.laucoin.registry.audit") as Logger
	private val appender = ListAppender<ILoggingEvent>()

	@BeforeEach
	fun attachAppender() {
		appender.start()
		auditLogger.addAppender(appender)
	}

	@AfterEach
	fun detachAppender() {
		auditLogger.detachAppender(appender)
		appender.stop()
	}

	@Test
	fun `Should audit record the correlation id read from the Reactor context`() {
		// Arrange
		val wrapped = service.audit(Mono.just("payload"), actor, USER_DELETE, userId)
			.contextWrite(Context.of(CORRELATION_ID_CONTEXT_KEY, "corr-123"))

		// Act
		StepVerifier.create(wrapped)
			.expectNext("payload")
			.verifyComplete()

		// Assert
		val entry = appender.list.single().formattedMessage
		assertTrue(entry.contains("\"correlationId\":\"corr-123\""))
		assertTrue(entry.contains("\"actorSub\":\"$userOidcId\""))
		assertTrue(entry.contains("\"actorId\":\"$userId\""))
	}

	@Test
	fun `Should audit record a single SUCCESS entry when the wrapped pipeline succeeds`() {
		// Arrange
		val wrapped = service.audit(Mono.just("payload"), actor, USER_DELETE, userId)

		// Act
		StepVerifier.create(wrapped)
			.expectNext("payload")
			.verifyComplete()

		// Assert
		assertEquals(1, appender.list.size)
		val entry = appender.list.single().formattedMessage
		assertTrue(entry.contains("\"action\":\"USER_DELETE\""))
		assertTrue(entry.contains("\"targetId\":\"$userId\""))
		assertTrue(entry.contains("\"outcome\":\"SUCCESS\""))
		assertFalse(entry.contains("correlationId"))
	}

	@Test
	fun `Should audit record a typed FAILURE entry and pass the business error through unaltered`() {
		// Arrange
		val error = RegistryException(CONFLICT, "ERROR_MESSAGE")
		val wrapped = service.audit(Mono.error<String>(error), actor, USER_DELETE, userId)

		// Act
		StepVerifier.create(wrapped)
			.expectErrorMatches { it === error }
			.verify(Duration.ofSeconds(5))

		// Assert
		assertEquals(1, appender.list.size)
		assertTrue(appender.list.single().formattedMessage.contains("\"outcome\":\"FAILURE:RegistryException\""))
	}

	@Test
	fun `Should audit keep the wrapped pipeline intact when the audit emission itself throws`() {
		// Arrange
		val explosiveTargetId = object {
			override fun toString(): String = throw IllegalStateException("boom")
		}
		val wrapped = service.audit(Mono.just("payload"), actor, USER_DELETE, explosiveTargetId)

		// Act
		StepVerifier.create(wrapped)
			.expectNext("payload")
			.verifyComplete()

		// Assert
		assertTrue(appender.list.isEmpty())
	}

	@Test
	fun `Should audit record a SUCCESS entry when the wrapped pipeline completes empty`() {
		// Arrange
		val wrapped = service.audit(Mono.empty<String>(), actor, USER_DELETE, userId)

		// Act
		StepVerifier.create(wrapped)
			.verifyComplete()

		// Assert
		assertEquals(1, appender.list.size)
		assertTrue(appender.list.single().formattedMessage.contains("\"outcome\":\"SUCCESS\""))
	}

	@Test
	fun `Should audit record a single CANCELLED entry when the wrapped pipeline is cancelled`() {
		// Arrange
		val wrapped = service.audit(Mono.never<String>(), actor, USER_DELETE, userId)

		// Act
		StepVerifier.create(wrapped)
			.thenCancel()
			.verify(Duration.ofSeconds(5))

		// Assert
		assertEquals(1, appender.list.size)
		val entry = appender.list.single().formattedMessage
		assertTrue(entry.contains("\"outcome\":\"CANCELLED\""))
		assertTrue(entry.contains("\"action\":\"USER_DELETE\""))
		assertTrue(entry.contains("\"targetId\":\"$userId\""))
	}
}
