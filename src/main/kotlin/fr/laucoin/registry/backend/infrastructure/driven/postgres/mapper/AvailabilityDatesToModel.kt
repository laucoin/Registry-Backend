package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import org.mapstruct.Mapping

/**
 * Objectif : Mappe la fenêtre de disponibilité d'une entité aplatie (date et heure) vers le modèle.
 * Périmètre : S'applique aux méthodes entité vers modèle dont l'entité porte les colonnes startAvailability* et endAvailability*.
 * Limites : Suppose un paramètre source nommé entity ; ne calcule aucun statut.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Mapping(
	target = "startAvailability",
	expression = "java(mapCustomDateTime(entity.getStartAvailabilityDate(), entity.getStartAvailabilityTime()))",
)
@Mapping(
	target = "endAvailability",
	expression = "java(mapCustomDateTime(entity.getEndAvailabilityDate(), entity.getEndAvailabilityTime()))",
)
annotation class AvailabilityDatesToModel
