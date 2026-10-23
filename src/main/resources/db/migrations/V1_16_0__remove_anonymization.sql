-- Anonymization is replaced by a plain deletion: remove already anonymized rows and the flag
DELETE FROM tb_participant WHERE purged IS TRUE;
DELETE FROM tb_user WHERE purged IS TRUE;

ALTER TABLE tb_participant DROP COLUMN purged;
ALTER TABLE tb_user DROP COLUMN purged;
