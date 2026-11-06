package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.extension.DateExt.isMajor
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.time.Clock
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Indique si un participant est majeur à l'instant courant.
 * Périmètre : Compare la date de naissance à l'horloge injectée.
 * Limites : Ne stocke rien.
 */
@Component
class AgeMappingHelper(private val clock: Clock) {
	@Named("major")
	fun major(model: ParticipantModel): Boolean = model.birthday.isMajor(clock)
}
