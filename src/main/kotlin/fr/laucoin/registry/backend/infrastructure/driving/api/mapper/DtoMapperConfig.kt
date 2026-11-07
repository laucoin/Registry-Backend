package fr.laucoin.registry.backend.infrastructure.driving.api.mapper

import org.mapstruct.InjectionStrategy.CONSTRUCTOR
import org.mapstruct.MapperConfig
import org.mapstruct.NullValueCheckStrategy.ALWAYS

/**
 * Objectif : Configuration commune des mappers MapStruct entre DTO de l'API et modèles du domaine.
 * Périmètre : Injection par constructeur et contrôle de nullité systématique : une valeur absente ne remplace jamais la valeur par défaut du modèle.
 * Limites : Ne porte aucun mapping ; chaque mapper déclare le sien.
 */
@MapperConfig(
	injectionStrategy = CONSTRUCTOR,
	nullValueCheckStrategy = ALWAYS,
)
interface DtoMapperConfig
