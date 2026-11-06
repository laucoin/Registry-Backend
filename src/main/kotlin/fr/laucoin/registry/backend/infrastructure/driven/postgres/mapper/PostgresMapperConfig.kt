package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.ActivityMappingHelper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.AuditMappingHelper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.AvailabilityMappingHelper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.ProjectMappingHelper
import org.mapstruct.InjectionStrategy.CONSTRUCTOR
import org.mapstruct.MapperConfig
import org.mapstruct.NullValueCheckStrategy.ALWAYS
import org.mapstruct.ReportingPolicy.ERROR

/**
 * Objectif : Configuration commune des mappers MapStruct entre entités PostgreSQL et modèles du domaine.
 * Périmètre : Injection par constructeur, contrôle de nullité systématique et rappels d'audit partagés.
 * Limites : Ne porte aucun mapping ; chaque mapper déclare le sien.
 */
@MapperConfig(
	uses = [AuditMappingHelper::class, AvailabilityMappingHelper::class, ActivityMappingHelper::class, ProjectMappingHelper::class],
	injectionStrategy = CONSTRUCTOR,
	nullValueCheckStrategy = ALWAYS,
	unmappedTargetPolicy = ERROR,
)
interface PostgresMapperConfig
