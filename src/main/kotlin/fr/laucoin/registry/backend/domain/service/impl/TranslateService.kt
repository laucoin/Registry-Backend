package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.util.Locale
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

/**
 * [ITranslateService] implementation delegating to the two [MessageSource] beans wired in
 * [I18nConfig] (`messagesSource`/`errorsSource`), falling back to the provided default (or the code
 * itself) when a key is missing from the bundle.
 */
@Component
class TranslateService(
	private val messagesSource: MessageSource,
	private val errorsSource: MessageSource,
): ITranslateService {
	override fun getMessage(
		code: String,
		args: Array<Any>?,
		default: String?,
		locale: Locale,
	): String {
		return messagesSource.getMessage(code, args, default ?: code, locale)!!
	}

	override fun getError(
		code: String,
		args: Array<Any>?,
		default: String?,
		locale: Locale,
	): String {
		return errorsSource.getMessage(code, args, default ?: code, locale)!!
	}
}