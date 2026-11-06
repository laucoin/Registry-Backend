package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import org.mapstruct.Mapping

/**
 * Objectif : Aplatit la fenêtre de disponibilité du modèle en colonnes date et heure de l'entité.
 * Périmètre : S'applique aux méthodes modèle vers entité dont l'entité porte les colonnes startAvailability* et endAvailability*.
 * Limites : Ne porte aucune règle de validité des dates.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Mapping(target = "startAvailabilityDate", source = "startAvailability.date")
@Mapping(target = "startAvailabilityTime", source = "startAvailability.time")
@Mapping(target = "endAvailabilityDate", source = "endAvailability.date")
@Mapping(target = "endAvailabilityTime", source = "endAvailability.time")
annotation class AvailabilityDatesToEntity
