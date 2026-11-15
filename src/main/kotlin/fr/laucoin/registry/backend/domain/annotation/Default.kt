package fr.laucoin.registry.backend.domain.annotation

/**
 * Objectif : Désigne le constructeur principal d'un modèle immuable que les outils de mapping doivent utiliser.
 * Périmètre : Évite le constructeur sans argument généré par Kotlin quand tous les paramètres ont une valeur par défaut.
 * Limites : Simple marqueur, sans dépendance à une bibliothèque de mapping.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
annotation class Default
