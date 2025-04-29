package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IActivityService {
    fun findActivitiesPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: ActivitySearchParamModel,
    ): Mono<PageModel<ActivityModel>>

    fun findActivityById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityModel>

    fun findActivityMovementsPage(
        projectId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun createActivity(currentUser: CurrentUserModel, activity: ActivityModel): Mono<ActivityModel>
    fun updateActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID, activity: ActivityModel): Mono<ActivityModel>
    fun disableActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ActivityModel>
    fun enableActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ActivityModel>
    fun deleteActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void>
}
