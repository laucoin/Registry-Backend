package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.model.MovementEventModel
import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.TimelineWindowModel
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Objectif : Déduit les sorties d'une entité (participant ou véhicule) de ses mouvements, ordonnés dans le temps, et en mesure la durée.
 * Périmètre : Une sortie va d'un mouvement OUT au mouvement IN suivant ; une sortie encore ouverte court jusqu'à maintenant ; le taux d'utilisation compte le temps en sortie dans une fenêtre.
 * Limites : Un départ définitif n'est pas une sortie (il referme seulement celle qui serait ouverte) ; ne lit aucune donnée.
 */
object OutingExt {
	fun List<MovementEventModel>.toOutings(now: ZonedDateTime): List<OutingModel> {
		val outings = mutableListOf<OutingModel>()
		var openedAt: ZonedDateTime? = null

		for (event in this) {
			closedOuting(openedAt, event)?.let(outings::add)
			openedAt = nextOpening(openedAt, event)
		}

		return openedAt?.let { outings + OutingModel(it, now, ongoing = true) } ?: outings
	}

	private fun closedOuting(openedAt: ZonedDateTime?, event: MovementEventModel): OutingModel? {
		if (openedAt == null || !event.closesOuting()) return null

		return OutingModel(openedAt, event.dateTime)
	}

	private fun MovementEventModel.closesOuting(): Boolean {
		return type == IN || (type == OUT && reason == DEFINITIVE_DEPARTURE)
	}

	fun List<OutingModel>.outSeconds(window: TimelineWindowModel?): Long {
		return sumOf { window?.overlapSeconds(it) ?: Duration.between(it.start, it.end).seconds }
	}

	fun List<OutingModel>.usageRate(window: TimelineWindowModel?): Double? {
		return window?.seconds?.takeIf { it > 0 }?.let { outSeconds(window).toDouble() / it }
	}

	private fun nextOpening(openedAt: ZonedDateTime?, event: MovementEventModel): ZonedDateTime? {
		return when {
			event.type == IN || event.reason == DEFINITIVE_DEPARTURE -> null
			else -> openedAt ?: event.dateTime
		}
	}
}
