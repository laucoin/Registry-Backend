package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import org.mapstruct.Mapping

/**
 * Objectif : Marque les colonnes d'audit de l'entité comme mappées par le rappel d'audit ou en lecture seule.
 * Périmètre : S'applique aux méthodes modèle vers entité ; les colonnes d'audit sont renseignées par AuditMappingHelper.
 * Limites : Ne mappe rien lui-même.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Mapping(target = "createdAt", ignore = true)
@Mapping(target = "creatorId", ignore = true)
@Mapping(target = "creatorFirstName", ignore = true)
@Mapping(target = "creatorLastName", ignore = true)
@Mapping(target = "creatorEmail", ignore = true)
@Mapping(target = "lastUpdateAt", ignore = true)
@Mapping(target = "lastEditorId", ignore = true)
@Mapping(target = "lastEditorFirstName", ignore = true)
@Mapping(target = "lastEditorLastName", ignore = true)
@Mapping(target = "lastEditorEmail", ignore = true)
@Mapping(target = "fullCount", ignore = true)
annotation class IgnoreEntityAudit
