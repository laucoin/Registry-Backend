CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- tb_user_search definition
ALTER TABLE tb_user
    ADD COLUMN search_text TEXT GENERATED ALWAYS
        AS (COALESCE(first_name, '') || ' ' || COALESCE(last_name, '') || ' ' || COALESCE(email, '')) STORED;

CREATE INDEX tb_user_index_search ON tb_user USING gin (search_text gin_trgm_ops);

-- tb_participant_search definition
ALTER TABLE tb_participant
    ADD COLUMN search_text TEXT GENERATED ALWAYS
        AS (COALESCE(first_name, '') || ' ' || COALESCE(last_name, '')) STORED;

CREATE INDEX tb_participant_index_search ON tb_participant USING gin (search_text gin_trgm_ops);

-- tb_vehicle_search definition
ALTER TABLE tb_vehicle
    ADD COLUMN search_text TEXT GENERATED ALWAYS
        AS (COALESCE(license_plate, '') || ' ' || COALESCE(brand, '') || ' ' || COALESCE(model, '')) STORED;

CREATE INDEX tb_vehicle_index_search ON tb_vehicle USING gin (search_text gin_trgm_ops);

-- tb_activity_search definition
ALTER TABLE tb_activity
    ADD COLUMN search_text TEXT GENERATED ALWAYS
        AS (COALESCE(name, '') || ' ' || COALESCE(description, '')) STORED;

CREATE INDEX tb_activity_index_search ON tb_activity USING gin (search_text gin_trgm_ops);
