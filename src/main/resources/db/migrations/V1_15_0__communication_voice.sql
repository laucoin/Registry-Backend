-- Communication voice: a message can speak "on behalf of the movement" (the
-- activity outing is the author) instead of the writing user. Backfill keeps
-- the historical display rule (a movement-linked message spoke as the movement).
ALTER TABLE tb_communication
    ADD COLUMN on_behalf_of_movement BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE tb_communication
SET on_behalf_of_movement = TRUE
WHERE movement_id IS NOT NULL;
