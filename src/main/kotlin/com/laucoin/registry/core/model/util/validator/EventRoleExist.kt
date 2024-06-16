package com.laucoin.registry.core.model.util.validator

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass

@Target(FIELD, PROPERTY_GETTER, VALUE_PARAMETER)
@Retention(RUNTIME)
@Constraint(validatedBy = [EventRoleValidator::class])
annotation class EventRoleExist(
    val message: String = "ROLE_NOT_FOUND",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
