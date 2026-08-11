package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AlertCreationWriterDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream

/**
 * Raising an alert may carry an opening message, and the field is optional on
 * the form. The mapper used to build the seed communication unconditionally, so
 * an alert raised with a title alone persisted a blank message — a row with an
 * author, a timestamp and nothing to read, which every later message in the
 * thread then had to be scrolled past.
 */
class AlertCreationWriterDtoMapperTest {
	private val mapper = AlertCreationWriterDtoMapper()

	private companion object {
		private val PROJECT_ID: UUID = UUID.randomUUID()
		private val MOVEMENT_ID: UUID = UUID.randomUUID()
		private val WHEN: ZonedDateTime = ZonedDateTime.parse("2026-08-10T14:30:00Z")

		private fun dto(message: String?, movementId: UUID? = null) = AlertCreationWriterDto(
			title = "Orage",
			dateTime = WHEN,
			message = message,
			movementId = movementId,
		)

		@JvmStatic
		fun `Should open no thread when there is nothing to say`(): Stream<Arguments> = Stream.of(
			Arguments.of(null),
			Arguments.of(""),
			Arguments.of("   "),
			Arguments.of("\t\n"),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should open no thread when there is nothing to say`(message: String?) {
		// Act
		val model = mapper.toModel(dto(message), PROJECT_ID)

		// Assert
		assertEquals(emptyList<Any>(), model.communications)
	}

	/**
	 * A movement is attached THROUGH the opening message — the link is the note
	 * about that outing — so with no message there is nothing to attach it to.
	 */
	@Test
	fun `Should open no thread when a movement is attached without a message`() {
		// Act
		val model = mapper.toModel(dto(message = null, movementId = MOVEMENT_ID), PROJECT_ID)

		// Assert
		assertEquals(emptyList<Any>(), model.communications)
	}

	@Test
	fun `Should open the thread with the message when one is written`() {
		// Act
		val model = mapper.toModel(dto("Le groupe est à l’abri", MOVEMENT_ID), PROJECT_ID)

		// Assert
		assertEquals(1, model.communications?.size)
		val communication = model.communications!!.first()
		assertEquals("Le groupe est à l’abri", communication.message)
		assertEquals(WHEN, communication.dateTime)
		assertEquals(MOVEMENT_ID, communication.movement?.id)
		assertEquals(PROJECT_ID, communication.project?.id)
	}

	@Test
	fun `Should trim the opening message`() {
		// Act
		val model = mapper.toModel(dto("  RAS  "), PROJECT_ID)

		// Assert
		assertEquals("RAS", model.communications?.first()?.message)
	}

	@Test
	fun `Should always open the alert in progress with the project attached`() {
		// Act
		val model = mapper.toModel(dto(null), PROJECT_ID)

		// Assert
		assertEquals("Orage", model.title)
		assertEquals(WHEN, model.dateTime)
		assertEquals(IN_PROGRESS, model.status)
		assertEquals(PROJECT_ID, model.project?.id)
		assertNull(model.id)
	}
}
