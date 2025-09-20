-- tb_preferences adding theme and language
ALTER TABLE tb_preferences
    ADD COLUMN theme    VARCHAR(10) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN language VARCHAR(5);
