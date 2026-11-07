package fr.laucoin.registry.backend.infrastructure.driven.idp.mapper

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.idp.entity.IdpTokenEntity
import org.mapstruct.Mapper

@Mapper
interface AuthenticationTokenEntityMapper: IEntityMapper<TokenModel, IdpTokenEntity>
