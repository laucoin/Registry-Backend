-- tb_participant_type definition
ALTER TABLE tb_participant
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'REGISTERED';

ALTER TABLE tb_participant
    ALTER COLUMN type DROP DEFAULT;
