-- tb_movement project scoped last movement lookup index
CREATE INDEX tb_movement_index_project_id_date_time ON tb_movement (project_id, date_time);
