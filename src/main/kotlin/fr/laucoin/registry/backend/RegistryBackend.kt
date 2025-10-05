package fr.laucoin.registry.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class RegistryBackend

fun main(args: Array<String>) {
	runApplication<RegistryBackend>(*args)
}
