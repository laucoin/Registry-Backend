package com.laucoin.registry.domain.profile.model

import com.laucoin.registry.core.model.util.GenericEventModel
import com.laucoin.registry.core.model.util.validator.EventRoleExist
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

open class ProfileModel(
    @field:NotNull
    var userId: UUID? = null,
    @field:NotNull
    @field:EventRoleExist
    var role: String? = null,
    var accepted: Boolean = false,
    var startAccess: LocalDateTime? = null,
    var endAccess: LocalDateTime? = null,
): GenericEventModel() {
    override fun filterFields(): List<String?> {
        val event = event()
        return listOf(role) + event.filterFields() +
               (event.address?.filterFields() ?: emptyList())
    }
}
