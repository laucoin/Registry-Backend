package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.test.TestContext
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder

class AlertCriticalityRuleMessagesTest : TestContext() {
	@Autowired
	private lateinit var mapper: AlertCriticalityMetadataReaderDtoMapper

	@AfterEach
	fun resetLocale() {
		LocaleContextHolder.resetLocaleContext()
	}

	@Test
	fun `Should toDto describe the rule in English from the configured thresholds`() {
		// Arrange
		LocaleContextHolder.setLocale(Locale.ENGLISH)

		// Act
		val result = mapper.toDto(AlertCriticalityEnum.entries)

		// Assert
		assertEquals("Automatic criticality by age: recent < 6 h · reinforced 6–18 h · high > 18 h", result.rule)
	}

	@Test
	fun `Should toDto describe the rule in French from the configured thresholds`() {
		// Arrange
		LocaleContextHolder.setLocale(Locale.FRENCH)

		// Act
		val result = mapper.toDto(AlertCriticalityEnum.entries)

		// Assert
		assertEquals("Criticité automatique selon l'ancienneté : récente < 6 h · renforcée 6–18 h · élevée > 18 h", result.rule)
	}
}
