-- "Gone for good" is a fact of the register, not a property of the availability
-- window. It used to be recorded by overwriting end_availability_*, which made
-- an expired stay and a definitive departure indistinguishable and destroyed
-- any date the staff had entered ahead of time.
ALTER TABLE tb_participant
    ADD COLUMN departed_at TIMESTAMP WITH TIME ZONE;

-- Every referential read (eligible participants, group expansion, head counts)
-- filters on "not departed", so the partial index carries those.
CREATE INDEX tb_participant_index_not_departed
    ON tb_participant (project_id) WHERE departed_at IS NULL;

-- Backfill from the movement history, with the same predicate as
-- MovementModel.isLastParticipantMovement(): a DEFINITIVE_DEPARTURE exit, or any
-- guest exit. Only the participant's LAST visible movement counts, so a
-- corrected history never marks someone as gone.
WITH last_movement AS (SELECT DISTINCT ON (mc.participant_id) mc.participant_id,
                                                              m.type,
                                                              m.reason,
                                                              m.date_time
                       FROM tb_movement_content mc
                                INNER JOIN tb_movement m ON m.id = mc.movement_id
                       WHERE m.visible IS TRUE
                       ORDER BY mc.participant_id, m.date_time DESC, m.created_date DESC)
UPDATE tb_participant p
SET departed_at = last_movement.date_time
FROM last_movement
WHERE last_movement.participant_id = p.id
  AND last_movement.type = 'OUT'
  AND (last_movement.reason = 'DEFINITIVE_DEPARTURE' OR p.type = 'GUEST');
