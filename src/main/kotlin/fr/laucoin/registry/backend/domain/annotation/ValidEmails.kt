package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.validator.ValidEmailsValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Class-level constraint that validates every element of a named collection-of-emails
 * field is a well-formed email address. A null or empty collection is considered valid
 * (emptiness is handled separately, e.g. by [AtLeastOneNonEmpty]). Implemented at class
 * level because Kotlin type-argument constraints (`List<@Email String>`) are not reliably
 * traversed by the validator here.
 */
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [ValidEmailsValidator::class])
annotation class ValidEmails(
	val field: String,
	val message: String,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
