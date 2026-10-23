package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX

/**
 * Kind of element whose availability status is labelled, so that each kind owns its own translations
 * (`availability.status.<subject>.*`) and can word them differently.
 */
enum class AvailabilitySubjectEnum {
	PROFILE,
	GROUP,
	ACTIVITY,
	PROJECT;

	val keyPrefix: String = "$AVAILABILITY_STATUS_PREFIX${name.lowercase()}."
}
