package fr.laucoin.registry.backend.domain.service

import java.util.Locale

interface ITranslateService {
    fun getMessage(
        code: String,
        locale: Locale,
        args: Array<Any>? = null,
        default: String? = null,
    ): String

    fun getError(
        code: String,
        locale: Locale,
        args: Array<Any>? = null,
        default: String? = null,
    ): String
}