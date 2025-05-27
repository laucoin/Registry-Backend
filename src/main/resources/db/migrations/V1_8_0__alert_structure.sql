-- tb_alert definition
CREATE TABLE tb_alert
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    date_time          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    title              VARCHAR(50),
    status             VARCHAR(50),
    project_id         uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_alert_pkey PRIMARY KEY (id)
);

-- tb_communication adding alert_id
ALTER TABLE tb_communication
    ADD COLUMN alert_id uuid;
ALTER TABLE tb_communication
    ALTER COLUMN movement_id DROP NOT NULL;

-- tb_alert foreign keys and indexes
CREATE INDEX tb_alert_index_project_id ON tb_alert (project_id);
CREATE INDEX tb_alert_index_created_by ON tb_alert (created_by);
CREATE INDEX tb_alert_index_last_modified_by ON tb_alert (last_modified_by);

ALTER TABLE tb_alert
    ADD CONSTRAINT tb_alert_project_fkey FOREIGN KEY (project_id) REFERENCES tb_project (id) ON DELETE CASCADE;
ALTER TABLE tb_alert
    ADD CONSTRAINT tb_alert_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_alert
    ADD CONSTRAINT tb_alert_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_communication foreign keys and indexes
ALTER TABLE tb_communication
    ADD CONSTRAINT tb_communication_alert_fkey FOREIGN KEY (alert_id) REFERENCES tb_alert (id) ON DELETE SET NULL;
