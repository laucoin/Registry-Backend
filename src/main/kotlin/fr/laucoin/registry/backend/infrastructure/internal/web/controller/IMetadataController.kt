package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.Locale
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Flux

@Tag(name = "Metadata", description = "API for global metadata")
@RequestMapping("/api/metadata")
interface IMetadataController {

    @Operation(
        summary = "Get presence element's status",
        description = "Get all presence element's status",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping("/presences/status")
    fun getPresencesStatus(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<LabelDto>

    @Operation(
        summary = "Get availabilities status",
        description = "Get all availabilities status",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping("/availabilities/status")
    fun getAvailabilitiesStatus(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<LabelDto>

    @Operation(
        summary = "Get profile's status",
        description = "Get all profile's status",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping("/profiles/status")
    fun getProjectProfileStatus(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<LabelDto>

    @Operation(
        summary = "Get Movement Type",
        description = "Get all movement type",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping("/movements/types")
    fun getMovementTypes(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<LabelDto>

    @Operation(
        summary = "Get Participant Type",
        description = "Get all participant type",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping("/participants/types")
    fun getParticipantTypes(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<LabelDto>

    @Operation(
        summary = "Get Alert Status",
        description = "Get all alert status",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @GetMapping("/alerts/status")
    fun getAlertStatus(@RequestHeader(ACCEPT_LANGUAGE) locale: Locale): Flux<LabelDto>
}
