package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX

/**
 * Type d'élément dont le statut de disponibilité est libellé, de sorte que chaque type possède ses propres traductions
 * (`availability.status.<subject>.*`) et puisse les formuler différemment.
 */
enum class AvailabilitySubjectEnum {
	PROFILE,
	GROUP,
	ACTIVITY,
	PROJECT;

	val keyPrefix: String = "$AVAILABILITY_STATUS_PREFIX${name.lowercase()}."
}
