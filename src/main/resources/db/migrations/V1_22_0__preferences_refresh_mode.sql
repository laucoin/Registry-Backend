-- tb_preferences adding refresh mode (automatic data refresh frequency)
ALTER TABLE tb_preferences
    ADD COLUMN refresh_mode VARCHAR(10) NOT NULL DEFAULT 'NORMAL';
