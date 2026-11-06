package fr.laucoin.registry.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Objectif : Point d'entrée de l'application Registry.
 * Périmètre : Démarre le contexte Spring Boot et active la planification des tâches.
 * Limites : Ne porte aucune configuration ; elle vit dans `config/` et dans les propriétés externes.
 */
@EnableScheduling
@SpringBootApplication
class RegistryBackend

fun main(args: Array<String>) {
	runApplication<RegistryBackend>(*args)
}
