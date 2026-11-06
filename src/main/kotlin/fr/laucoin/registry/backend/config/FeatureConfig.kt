package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.model.ProfileFeatureModel
import fr.laucoin.registry.backend.domain.model.CommunicationFeatureModel
import fr.laucoin.registry.backend.domain.model.ParticipantFeatureModel
import fr.laucoin.registry.backend.domain.model.MovementFeatureModel
import fr.laucoin.registry.backend.domain.model.PurgeFeatureModel
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Objectif : Active la configuration typée des fonctionnalités métier.
 * Périmètre : Enregistre les propriétés des mouvements (limites de recherche, planification, tolérance de date future), des communications, des participants et des profils (limites de recherche) et des purges (seuils de rétention).
 * Limites : Ne porte aucune valeur : elles viennent de la configuration externe.
 */
@Configuration
@EnableConfigurationProperties(
	CommunicationFeatureModel::class,
	MovementFeatureModel::class,
	ParticipantFeatureModel::class,
	ProfileFeatureModel::class,
	PurgeFeatureModel::class,
)
class FeatureConfig
