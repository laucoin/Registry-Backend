package fr.laucoin.registry.backend.domain.constant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.PropertyResourceBundle

/**
 * The error catalogue is a three-way contract and NOTHING checks it at compile
 * time: the constant's VALUE is the code the API returns, it is also the suffix
 * the message sources look up (`error.message.<value>`), and it has to exist in
 * both bundles. A single typo therefore does not fail a build — it silently
 * turns one refusal into "an unexpected error occurred", which is how an
 * invitation with an out-of-order access window came to explain nothing at all
 * (`PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS` held the value
 * `PROJECT_START_ACCESS_…`).
 *
 * Generated per constant so a failure names the offending one rather than
 * printing a list.
 */
class ErrorConstTest {
	private companion object {
		private fun bundle(locale: Locale): PropertyResourceBundle =
			PropertyResourceBundle.getBundle("i18n/errors", locale) as PropertyResourceBundle

		/**
		 * Every `const val` in ErrorConst and its nested error groups. Read
		 * through Java reflection because a Kotlin `const val` compiles to a
		 * STATIC field: its property getter takes no receiver, so the Kotlin
		 * reflection path rejects the object instance.
		 */
		private fun allCodes(): List<Pair<String, String>> {
			val holders = listOf(ErrorConst::class.java) + ErrorConst::class.java.declaredClasses
			return holders.flatMap { holder ->
				holder.declaredFields
					.filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
					.map { field ->
						field.isAccessible = true
						field.name to field.get(null) as String
					}
			}
		}
	}

	/**
	 * The constant's name and its value must agree: the name is what the code
	 * reads, the value is what the client and the bundles see, and a divergence
	 * between them is invisible until a user hits that exact refusal.
	 */
	@TestFactory
	fun `Should name every error code after its own value`(): List<DynamicTest> =
		allCodes().map { (name, value) ->
			DynamicTest.dynamicTest("$name is its own value") {
				// Act + Assert
				assertEquals(name, value)
			}
		}

	@TestFactory
	fun `Should translate every error code in both bundles`(): List<DynamicTest> =
		allCodes().flatMap { (name, value) ->
			listOf(Locale.ENGLISH, Locale.FRENCH).map { locale ->
				DynamicTest.dynamicTest("$name has a ${locale.language} message") {
					// Arrange
					val key = "${TranslationKeyConst.ERROR_MESSAGE_PREFIX}$value"

					// Act
					val translated = bundle(locale).handleGetObject(key)

					// Assert — a missing entry silently degrades to UNKNOWN_ERROR
					assertEquals(true, translated != null, "no $key in the ${locale.language} bundle")
				}
			}
		}
}
