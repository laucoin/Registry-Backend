-- Fix: V1_3_0__activity_structure.sql mistakenly created the tb_activity
-- foreign-key indexes on tb_vehicle (a copy/paste error). As a result:
--   * tb_activity has no index on project_id / created_by / last_modified_by,
--     hurting tenant filtering and cascade-delete performance, and
--   * tb_vehicle carries three redundant, misnamed indexes (it already has the
--     equivalent tb_vehicle_index_* indexes from V1_2_0).
-- Migrations are forward-only, so this corrects the state in a new migration
-- rather than editing V1_3_0.

-- Remove the misnamed indexes that landed on tb_vehicle in V1_3_0.
DROP INDEX IF EXISTS tb_activity_index_project_id;
DROP INDEX IF EXISTS tb_activity_index_created_by;
DROP INDEX IF EXISTS tb_activity_index_last_modified_by;

-- Create the intended indexes on tb_activity.
CREATE INDEX IF NOT EXISTS tb_activity_index_project_id ON tb_activity (project_id);
CREATE INDEX IF NOT EXISTS tb_activity_index_created_by ON tb_activity (created_by);
CREATE INDEX IF NOT EXISTS tb_activity_index_last_modified_by ON tb_activity (last_modified_by);
