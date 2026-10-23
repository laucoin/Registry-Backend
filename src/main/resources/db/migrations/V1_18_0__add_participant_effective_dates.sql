-- tb_participant effective arrival and departure
ALTER TABLE tb_participant
    ADD COLUMN effective_arrival   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN effective_departure TIMESTAMP WITH TIME ZONE;

UPDATE tb_participant p
SET effective_arrival = first_entry.entered_at
FROM (SELECT mc.participant_id, MIN(m.date_time) AS entered_at
      FROM tb_movement m
               INNER JOIN tb_movement_content mc ON mc.movement_id = m.id
      WHERE m.visible IS TRUE
        AND m.type = 'IN'
      GROUP BY mc.participant_id) first_entry
WHERE first_entry.participant_id = p.id;

UPDATE tb_participant p
SET effective_departure = last_exit.left_at
FROM (SELECT mc.participant_id, MAX(m.date_time) AS left_at
      FROM tb_movement m
               INNER JOIN tb_movement_content mc ON mc.movement_id = m.id
               INNER JOIN tb_participant tp ON tp.id = mc.participant_id
      WHERE m.visible IS TRUE
        AND m.type = 'OUT'
        AND (m.reason = 'DEFINITIVE_DEPARTURE' OR tp.type = 'GUEST')
      GROUP BY mc.participant_id) last_exit
WHERE last_exit.participant_id = p.id
  AND NOT EXISTS (SELECT 1
                  FROM tb_movement m2
                           INNER JOIN tb_movement_content mc2 ON mc2.movement_id = m2.id
                  WHERE mc2.participant_id = p.id
                    AND m2.visible IS TRUE
                    AND m2.type = 'IN'
                    AND m2.date_time > last_exit.left_at);
