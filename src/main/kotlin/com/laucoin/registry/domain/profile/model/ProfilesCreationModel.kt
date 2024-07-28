package com.laucoin.registry.domain.profile.model

import com.laucoin.registry.core.model.util.GenericEventModel
import com.laucoin.registry.core.model.util.validator.EventRoleExist
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

open class ProfilesCreationModel(
    @field:NotNull
    @field:NotEmpty
    var users: Set<String>? = null,
    @field:NotNull
    @field:EventRoleExist
    var role: String? = null,
    var startAccess: LocalDateTime? = null,
    var endAccess: LocalDateTime? = null,
): GenericEventModel() {
    override fun filterFields(): List<String?> {
        val event = event()
        return listOf(role) + event.filterFields() +
               (event.address?.filterFields() ?: emptyList())
    }
}
