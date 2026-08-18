-- Indexes for the API v2 list grammar, limited to the three unbounded,
-- chronologically-listed tables. Every tenant table already carries a
-- (project_id) index and the ?q= searches are served by the pg_trgm GIN
-- indexes (V1_4_0); at these per-project row counts a b-tree per sortable
-- column only taxes every write without a measurable read win.

CREATE INDEX tb_movement_index_project_id_date_time ON tb_movement (project_id, date_time);
CREATE INDEX tb_communication_index_project_id_date_time ON tb_communication (project_id, date_time);
CREATE INDEX tb_alert_index_project_id_date_time ON tb_alert (project_id, date_time);
