package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import org.mapstruct.Mapping

/**
 * Objectif : Conserve les colonnes génériques (identifiant, visibilité) lors d'un mapping partiel vers l'entité.
 * Périmètre : À combiner avec BeanMapping(ignoreByDefault) : seules les colonnes listées explicitement sont alors mappées.
 * Limites : Ne mappe aucune colonne d'audit ; elles sont gérées par AuditMappingHelper.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Mapping(target = "id", source = "id")
@Mapping(target = "visible", source = "visible")
annotation class KeepGenericColumns
