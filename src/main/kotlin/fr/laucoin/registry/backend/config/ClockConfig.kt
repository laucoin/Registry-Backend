package fr.laucoin.registry.backend.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Objectif : Expose l'horloge de l'application sous forme de bean.
 * Périmètre : Fournit l'unique `Clock` UTC que les services et les requêtes utilisent pour l'instant courant.
 * Limites : Ne porte aucune logique de fuseau horaire ; les appelants convertissent au besoin.
 */
@Configuration
class ClockConfig {
	@Bean
	fun clock(): Clock = Clock.systemUTC()
}
