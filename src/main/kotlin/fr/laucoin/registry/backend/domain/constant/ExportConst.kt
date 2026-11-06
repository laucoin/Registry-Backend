package fr.laucoin.registry.backend.domain.constant

/**
 * Objectif : Centralise les valeurs fixes des exports de données personnelles.
 * Périmètre : Version du schéma d'export et types de sujet exportés.
 * Limites : Ne contient aucune logique.
 */
object ExportConst {
	const val SCHEMA_VERSION = "1.0"
	const val SUBJECT_USER = "USER"
	const val SUBJECT_PARTICIPANT = "PARTICIPANT"
}
