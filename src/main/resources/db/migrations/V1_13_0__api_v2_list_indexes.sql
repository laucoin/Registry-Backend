-- ADR 018 §5 — indexes covering the API v2 list grammar (ADR 017 §5).
-- One index per whitelisted sort field (the *SortFieldEnum* → column mappings
-- in the postgres adapters); project-scoped collections get composite indexes
-- with the tenant column first, so a sorted page over one project never scans
-- another project's rows. The pg_trgm GIN indexes for ?q= already exist.

-- Users (global list) — email already carries a unique index (V1_1_0)
CREATE INDEX tb_user_index_last_name ON tb_user (last_name);
CREATE INDEX tb_user_index_first_name ON tb_user (first_name);
CREATE INDEX tb_user_index_role ON tb_user (role);
CREATE INDEX tb_user_index_last_login ON tb_user (last_login);

-- Projects (global list)
CREATE INDEX tb_project_index_name ON tb_project (name);
CREATE INDEX tb_project_index_begin_date ON tb_project (begin_date);
CREATE INDEX tb_project_index_end_date ON tb_project (end_date);

-- Participants
CREATE INDEX tb_participant_index_project_id_last_name ON tb_participant (project_id, last_name);
CREATE INDEX tb_participant_index_project_id_first_name ON tb_participant (project_id, first_name);
CREATE INDEX tb_participant_index_project_id_birthday ON tb_participant (project_id, birthday);
CREATE INDEX tb_participant_index_project_id_type ON tb_participant (project_id, type);

-- Groups
CREATE INDEX tb_group_index_project_id_name ON tb_group (project_id, name);
CREATE INDEX tb_group_index_project_id_start_availability_date ON tb_group (project_id, start_availability_date);
CREATE INDEX tb_group_index_project_id_end_availability_date ON tb_group (project_id, end_availability_date);

-- Movements
CREATE INDEX tb_movement_index_project_id_date_time ON tb_movement (project_id, date_time);
CREATE INDEX tb_movement_index_project_id_type ON tb_movement (project_id, type);
CREATE INDEX tb_movement_index_project_id_reason ON tb_movement (project_id, reason);

-- Vehicles
CREATE INDEX tb_vehicle_index_project_id_license_plate ON tb_vehicle (project_id, license_plate);
CREATE INDEX tb_vehicle_index_project_id_brand ON tb_vehicle (project_id, brand);
CREATE INDEX tb_vehicle_index_project_id_model ON tb_vehicle (project_id, model);

-- Activities
CREATE INDEX tb_activity_index_project_id_name ON tb_activity (project_id, name);
CREATE INDEX tb_activity_index_project_id_duration ON tb_activity (project_id, duration);
CREATE INDEX tb_activity_index_project_id_start_availability_date ON tb_activity (project_id, start_availability_date);
CREATE INDEX tb_activity_index_project_id_end_availability_date ON tb_activity (project_id, end_availability_date);

-- Communications (sorting by message text is served by the existing trigram
-- infrastructure; a btree over the full message column would be oversized)
CREATE INDEX tb_communication_index_project_id_date_time ON tb_communication (project_id, date_time);

-- Alerts
CREATE INDEX tb_alert_index_project_id_date_time ON tb_alert (project_id, date_time);
CREATE INDEX tb_alert_index_project_id_title ON tb_alert (project_id, title);
CREATE INDEX tb_alert_index_project_id_status ON tb_alert (project_id, status);
