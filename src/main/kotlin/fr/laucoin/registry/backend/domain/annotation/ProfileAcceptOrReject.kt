package fr.laucoin.registry.backend.domain.annotation

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_STATUS_NOT_ACCEPTED_OR_REJECTED
import fr.laucoin.registry.backend.domain.validator.ProfileAcceptOrRejectValidator
import jakarta.validation.Constraint
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass

@Repeatable
@Target(VALUE_PARAMETER)
@Retention(RUNTIME)
@Constraint(validatedBy = [ProfileAcceptOrRejectValidator::class])
annotation class ProfileAcceptOrReject(
	val message: String = PROJECT_PROFILE_STATUS_NOT_ACCEPTED_OR_REJECTED,
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Any>> = []
)
