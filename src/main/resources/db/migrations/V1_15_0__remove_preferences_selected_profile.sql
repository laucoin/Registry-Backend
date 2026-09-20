ALTER TABLE tb_preferences
    DROP CONSTRAINT tb_preferences_default_profile_fkey;

DROP INDEX tb_preferences_index_selected_profile_id;

ALTER TABLE tb_preferences
    DROP COLUMN selected_profile_id;
