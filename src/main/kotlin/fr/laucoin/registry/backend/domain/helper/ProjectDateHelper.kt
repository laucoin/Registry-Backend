package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IProjectPort
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Objectif : Vérifie que les dates d'un élément restent dans la plage de dates de son projet.
 * Périmètre : Charge le projet par son port et rejette toute date hors plage par une erreur de domaine.
 * Limites : Ne modifie aucune donnée et ne porte aucune autre règle de l'élément validé.
 */
@Component
class ProjectDateHelper(private val port: IProjectPort) {
	private val log = LoggerFactory.getLogger(ProjectDateHelper::class.java)

	fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID> {
		return findProject(id)
			.handle { project, handle ->
				if (project.isNotInRange(dateTime)) {
					log.warn(
						"Failed to editing, date {} is out of project range [{}, {}]",
						dateTime,
						project.begin,
						project.end,
					)
					handle.error(
						RegistryException(
							status = UNPROCESSABLE_CONTENT,
							code = errorCode,
							args = arrayListOf(dateTime.toString(), project.begin.toString(), project.end.toString()),
						)
					)
				} else handle.next(id)
			}
	}

	fun validateDateTimes(
		id: UUID,
		start: CustomDateTimeModel?,
		end: CustomDateTimeModel?,
		errorCode: String,
	): Mono<UUID> {
		return findProject(id)
			.handle { project, handle ->
				if (project.isNotInRange(start) || project.isNotInRange(end)) {
					handle.error(outOfRange(project, start, end, errorCode))
				} else handle.next(id)
			}
	}

	private fun findProject(id: UUID): Mono<ProjectModel> {
		return port.findById(id, isVisible = null).notFoundIfEmpty(id)
	}

	private fun outOfRange(
		project: ProjectModel,
		start: CustomDateTimeModel?,
		end: CustomDateTimeModel?,
		errorCode: String,
	): RegistryException {
		log.warn(
			"Failed to editing, one or more dates ({}, {}) are out of project range [{}, {}]",
			start,
			end,
			project.begin,
			project.end
		)
		return RegistryException(
			status = UNPROCESSABLE_CONTENT,
			code = errorCode,
			args = arrayListOf(start.toString(), end.toString(), project.begin.toString(), project.end.toString()),
		)
	}
}
