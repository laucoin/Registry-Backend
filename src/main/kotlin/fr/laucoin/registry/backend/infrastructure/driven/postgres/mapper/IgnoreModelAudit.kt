package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import org.mapstruct.Mapping

/**
 * Objectif : Marque les champs d'historique du modèle comme mappés par le rappel d'audit.
 * Périmètre : S'applique aux méthodes entité vers modèle ; l'historique est renseigné par AuditMappingHelper.
 * Limites : Ne mappe rien lui-même.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Mapping(target = "creation", ignore = true)
@Mapping(target = "lastEdition", ignore = true)
annotation class IgnoreModelAudit
