package com.laucoin.registry.core.model.profile

import com.laucoin.registry.core.model.util.GenericEventModel
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(profileTable)
data class UserProfileModel(
    @Column(profileUserIdField)
    val userId: UUID? = null,
    var role: String? = null,
    var accepted: Boolean = false,
    @Column(profileStartAccessField)
    var startAccess: LocalDateTime? = null,
    @Column(profileEndAccessField)
    var endAccess: LocalDateTime? = null,
): GenericEventModel()
