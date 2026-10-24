package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.ProfileAcceptOrReject
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Purpose: Validator of the ProfileAcceptOrReject constraint.
 * Scope: Accepts only the statuses a user may set on an invitation.
 * Limits: Does not check the invitation itself.
 */
class ProfileAcceptOrRejectValidator: ConstraintValidator<ProfileAcceptOrReject, ProfileStatusEnum> {
	override fun isValid(value: ProfileStatusEnum, context: ConstraintValidatorContext): Boolean {
		return listOf(ACCEPTED, REJECTED).contains(value)
	}
}
