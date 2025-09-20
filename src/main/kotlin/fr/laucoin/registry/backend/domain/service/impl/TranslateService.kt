package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.util.Locale
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class TranslateService(
	private val messagesSource: MessageSource,
	private val errorsSource: MessageSource,
): ITranslateService {
	override fun getMessage(
		code: String,
		locale: Locale,
		args: Array<Any>?,
		default: String?,
	): String {
		return messagesSource.getMessage(code, args, default ?: code, locale)!!
	}

	override fun getError(
		code: String,
		locale: Locale,
		args: Array<Any>?,
		default: String?,
	): String {
		return errorsSource.getMessage(code, args, default ?: code, locale)!!
	}
}