-- tb_alert_search definition
ALTER TABLE tb_alert
    ADD COLUMN search_text TEXT GENERATED ALWAYS
        AS (COALESCE(title, '')) STORED;

CREATE INDEX tb_alert_index_search ON tb_alert USING gin (search_text gin_trgm_ops);

-- tb_communication_search definition
ALTER TABLE tb_communication
    ADD COLUMN search_text TEXT GENERATED ALWAYS
        AS (COALESCE(message, '')) STORED;

CREATE INDEX tb_communication_index_search ON tb_communication USING gin (search_text gin_trgm_ops);
