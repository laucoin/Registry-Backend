-- tb_preferences removing selected profile
DROP INDEX IF EXISTS tb_preferences_index_selected_profile_id;

ALTER TABLE tb_preferences
    DROP COLUMN selected_profile_id;
