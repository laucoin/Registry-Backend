package fr.laucoin.registry.backend.infrastructure.driving.security.service

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.ErrorDto
import java.util.Locale
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

/**
 * Objectif : Écrit la réponse 429 des limiteurs de débit.
 * Périmètre : Porte le statut, l'en-tête Retry-After et le corps problem+json traduit dans la locale de la requête.
 * Limites : Ne décide pas du refus ; l'appelant l'invoque lorsque sa capacité est dépassée.
 */
class TooManyRequestsResponder(
	private val translateService: ITranslateService,
	private val localeContextResolver: LocaleContextResolver,
	private val objectMapper: ObjectMapper,
) {
	fun respond(exchange: ServerWebExchange, retryAfterSeconds: Long): Mono<Void> {
		val response = exchange.response
		response.statusCode = TOO_MANY_REQUESTS
		response.headers.contentType = APPLICATION_PROBLEM_JSON
		response.headers.add(RETRY_AFTER, retryAfterSeconds.toString())

		val locale = localeContextResolver.resolveLocaleContext(exchange).locale ?: Locale.getDefault()
		val error = ErrorDto(
			title = translateService.getError(code = "$ERROR_TITLE_PREFIX${TOO_MANY_REQUESTS.value()}", locale = locale),
			status = TOO_MANY_REQUESTS.value(),
			detail = translateService.getError(code = "$ERROR_MESSAGE_PREFIX${TOO_MANY_REQUESTS.value()}", locale = locale),
			instance = exchange.request.path.value(),
			code = TOO_MANY_REQUESTS.value().toString(),
		)

		return response.writeWith(Mono.just(response.bufferFactory().wrap(objectMapper.writeValueAsBytes(error))))
	}
}
