package fr.laucoin.registry.backend.domain.service

/**
 * Objectif : Contrat d'accès à l'état des fonctionnalités activables par configuration.
 * Périmètre : Expose les indicateurs de fonctionnalité aux contrôleurs et aux autres services.
 * Limites : Contrat uniquement ; la source des indicateurs est portée par l'implémentation.
 */
interface IFeatureService {
	fun isInviteByEmailEnabled(): Boolean
}
