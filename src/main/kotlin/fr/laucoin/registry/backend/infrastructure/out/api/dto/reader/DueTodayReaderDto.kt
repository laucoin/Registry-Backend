package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

/**
 * One "due today" dashboard panel: the participants expected in (or out) today
 * and the groups whose own window opens (or closes) today. Both sides come from
 * one call because the panel shows them together — and because the two queries
 * behind it are issued concurrently, which only pays off if the client asks
 * once.
 */
data class DueTodayReaderDto(
	var participants: List<ParticipantReaderDto>,
	var groups: List<GroupReaderDto>,
)
