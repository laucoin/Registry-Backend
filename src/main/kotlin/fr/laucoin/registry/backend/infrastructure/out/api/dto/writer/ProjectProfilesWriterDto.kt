package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import fr.laucoin.registry.backend.domain.annotation.AtLeastOneNonEmpty
import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.annotation.ValidEmails
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_COLLECTION_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_EMAIL_INVALID
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_ROLE_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_USERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectProfileError.PROJECT_PROFILE_USERS_SIZE_IS_UPPER_THAN_MAX
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * Create project profiles for existing users (`userIds`) and/or invite people by
 * email (`emails`). An email with no matching account creates an email-only user
 * (invitation) that is linked on first OIDC login. At least one of the two lists
 * must be non-empty (see [AtLeastOneNonEmpty]).
 */
@StartBeforeEnd(
	startField = "startAccess",
	endField = "endAccess",
	message = PROJECT_PROFILE_START_ACCESS_LATER_THAN_END_ACCESS
)
@AtLeastOneNonEmpty(
	fields = ["userIds", "emails"],
	message = PROJECT_PROFILE_USERS_EMPTY
)
@ValidEmails(
	field = "emails",
	message = PROJECT_PROFILE_EMAIL_INVALID
)
data class ProjectProfilesWriterDto(
	@field:Size(max = DEFAULT_COLLECTION_LIMIT, message = PROJECT_PROFILE_USERS_SIZE_IS_UPPER_THAN_MAX)
	var userIds: List<UUID>? = null,
	@field:Size(max = DEFAULT_COLLECTION_LIMIT, message = PROJECT_PROFILE_USERS_SIZE_IS_UPPER_THAN_MAX)
	var emails: List<String>? = null,
	@field:NotBlank(message = PROJECT_PROFILE_ROLE_BLANK)
	var role: String? = null,
	@field:Valid
	var startAccess: CustomDateTimeWriterDto? = null,
	@field:Valid
	var endAccess: CustomDateTimeWriterDto? = null,
)
