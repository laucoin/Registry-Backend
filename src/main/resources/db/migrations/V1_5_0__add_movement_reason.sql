CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- tb_movement_reason definition
ALTER TABLE tb_movement
    ADD COLUMN reason VARCHAR(50);
