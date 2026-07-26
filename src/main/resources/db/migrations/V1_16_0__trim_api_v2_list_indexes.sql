-- Trim the V1_13_0 per-sort-field indexes (PR review): every tenant table
-- already carries a (project_id) index and the ?q= searches are served by the
-- pg_trgm GIN indexes (V1_4_0), so with small per-project row counts a b-tree
-- per sortable column only taxes every write without a measurable read win.
-- Kept: the (project_id, date_time) composites of the three unbounded,
-- chronologically-listed tables (movements, communications, alerts).
-- V1_13_0 is already applied to running databases, so the trim is a new
-- migration instead of an edit (Flyway checksum).

-- Users
DROP INDEX IF EXISTS tb_user_index_last_name;
DROP INDEX IF EXISTS tb_user_index_first_name;
DROP INDEX IF EXISTS tb_user_index_role;
DROP INDEX IF EXISTS tb_user_index_last_login;

-- Projects
DROP INDEX IF EXISTS tb_project_index_name;
DROP INDEX IF EXISTS tb_project_index_begin_date;
DROP INDEX IF EXISTS tb_project_index_end_date;

-- Participants
DROP INDEX IF EXISTS tb_participant_index_project_id_last_name;
DROP INDEX IF EXISTS tb_participant_index_project_id_first_name;
DROP INDEX IF EXISTS tb_participant_index_project_id_birthday;
DROP INDEX IF EXISTS tb_participant_index_project_id_type;

-- Groups
DROP INDEX IF EXISTS tb_group_index_project_id_name;
DROP INDEX IF EXISTS tb_group_index_project_id_start_availability_date;
DROP INDEX IF EXISTS tb_group_index_project_id_end_availability_date;

-- Movements (keeps tb_movement_index_project_id_date_time)
DROP INDEX IF EXISTS tb_movement_index_project_id_type;
DROP INDEX IF EXISTS tb_movement_index_project_id_reason;

-- Vehicles
DROP INDEX IF EXISTS tb_vehicle_index_project_id_license_plate;
DROP INDEX IF EXISTS tb_vehicle_index_project_id_brand;
DROP INDEX IF EXISTS tb_vehicle_index_project_id_model;

-- Activities
DROP INDEX IF EXISTS tb_activity_index_project_id_name;
DROP INDEX IF EXISTS tb_activity_index_project_id_duration;
DROP INDEX IF EXISTS tb_activity_index_project_id_start_availability_date;
DROP INDEX IF EXISTS tb_activity_index_project_id_end_availability_date;

-- Communications keeps tb_communication_index_project_id_date_time

-- Alerts (keeps tb_alert_index_project_id_date_time)
DROP INDEX IF EXISTS tb_alert_index_project_id_title;
DROP INDEX IF EXISTS tb_alert_index_project_id_status;
