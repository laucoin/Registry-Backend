package fr.laucoin.registry.backend.domain.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.ZonedDateTime

data class ParticipantModel(
    var firstName: String? = null,
    var lastName: String? = null,
    var birthday: LocalDate? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
    var user: UserModel? = null,
    var purged: Boolean? = null,
): GenericEventModel() {
    override fun getSearchableValues(): List<String> = listOfNotNull(firstName, lastName)

    @get:JsonProperty
    val major: Boolean
        get() {
            val now = LocalDate.now()
            val minBirthday = now.minusYears(18)
            return birthday?.let {
                minBirthday.isAfter(it)
                || minBirthday.isEqual(it)
            } ?: false
        }
}
