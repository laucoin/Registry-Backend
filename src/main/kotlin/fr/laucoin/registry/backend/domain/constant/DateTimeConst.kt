package fr.laucoin.registry.backend.domain.constant

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Objectif : Fournit la valeur d'attente des dates dont l'instant réel est toujours posé par l'appelant.
 * Périmètre : Expose l'époque UTC, qui remplace toute lecture implicite de l'horloge dans les valeurs par défaut des modèles.
 * Limites : Ne lit jamais l'horloge ; l'instant courant vient du bean `Clock` injecté.
 */
object DateTimeConst {
	val EPOCH_UTC: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
}
