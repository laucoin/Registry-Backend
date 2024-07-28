package com.laucoin.registry.core.model.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import java.time.LocalDateTime

@JsonInclude(NON_NULL)
data class HistoryModel(
    var date: LocalDateTime? = null,
    var user: HistoryUserModel = HistoryUserModel(),
)
