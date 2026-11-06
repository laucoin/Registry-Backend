package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.MinUpperMax
import jakarta.validation.ConstraintValidatorContext
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

/**
 * Objectif : Validateur de la contrainte MinUpperMax.
 * Périmètre : Compare deux champs numériques et valide lorsque le minimum n'est pas supérieur au maximum.
 * Limites : Supporte Int, Long, Float et Double ; toute autre paire est une erreur serveur.
 */
class MinUpperMaxValidator: GenericValidator<MinUpperMax, Any>() {
	private lateinit var startField: String
	private lateinit var endField: String

	override fun initialize(constraintAnnotation: MinUpperMax) {
		startField = constraintAnnotation.startField
		endField = constraintAnnotation.endField
	}

	override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
		val startValue = extractValue(startField, value)
		val endValue = extractValue(endField, value)

		if (startValue == null || endValue == null) return true

		return isOrdered(startValue, endValue)
	}

	private fun isOrdered(startValue: Any, endValue: Any): Boolean {
		if (startValue::class != endValue::class || startValue::class !in SUPPORTED_TYPES) {
			throw unsupportedTypes(startValue, endValue)
		}

		@Suppress("UNCHECKED_CAST")
		return (startValue as Comparable<Any>) <= endValue
	}

	private companion object {
		val SUPPORTED_TYPES = setOf(Int::class, Double::class, Float::class, Long::class)
	}

	private fun unsupportedTypes(startValue: Any?, endValue: Any?): RegistryException {
		val exception = RegistryException(
			INTERNAL_SERVER_ERROR,
			COMPARING_WRONG_PARAMETER_TYPE,
			arrayListOf(startValue, endValue),
		)
		log.error(
			"The two fields ({}, {}) are not of the same type or the type is not supported (we support only nullable ZonedDateTime and ZonedDateTime).",
			startValue,
			endValue,
			exception
		)
		return exception
	}
}
