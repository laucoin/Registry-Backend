package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.model.MovementFeatureModel
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Objectif : Active la configuration typée des fonctionnalités métier.
 * Périmètre : Enregistre les propriétés des mouvements (limites de recherche, planification, tolérance de date future).
 * Limites : Ne porte aucune valeur : elles viennent de la configuration externe.
 */
@Configuration
@EnableConfigurationProperties(MovementFeatureModel::class)
class FeatureConfig
