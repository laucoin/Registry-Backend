package fr.laucoin.registry.backend.domain.service

interface ITranslateService {
	fun getMessage(
		code: String,
		args: Array<Any>? = null,
		default: String? = null,
	): String

	fun getError(
		code: String,
		args: Array<Any>? = null,
		default: String? = null,
	): String
}