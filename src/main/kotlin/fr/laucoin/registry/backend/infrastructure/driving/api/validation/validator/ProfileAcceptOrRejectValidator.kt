package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.ProfileAcceptOrReject
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ProfileAcceptOrRejectValidator: ConstraintValidator<ProfileAcceptOrReject, ProfileStatusEnum> {
	override fun isValid(value: ProfileStatusEnum, context: ConstraintValidatorContext): Boolean {
		return listOf(ACCEPTED, REJECTED).contains(value)
	}
}
