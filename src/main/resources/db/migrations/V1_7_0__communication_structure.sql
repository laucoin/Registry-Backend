-- tb_communication definition
CREATE TABLE tb_communication
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    date_time          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    movement_id        uuid                     NOT NULL,
    message            VARCHAR(250),
    project_id         uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_communication_pkey PRIMARY KEY (id)
);

-- tb_communication foreign keys and indexes
CREATE INDEX tb_communication_index_movement_id ON tb_communication (movement_id);
CREATE INDEX tb_communication_index_project_id ON tb_communication (project_id);
CREATE INDEX tb_communication_index_created_by ON tb_communication (created_by);
CREATE INDEX tb_communication_index_last_modified_by ON tb_communication (last_modified_by);

ALTER TABLE tb_communication
    ADD CONSTRAINT tb_communication_movement_fkey FOREIGN KEY (movement_id) REFERENCES tb_movement (id) ON DELETE CASCADE;
ALTER TABLE tb_communication
    ADD CONSTRAINT tb_communication_project_fkey FOREIGN KEY (project_id) REFERENCES tb_project (id) ON DELETE CASCADE;
ALTER TABLE tb_communication
    ADD CONSTRAINT tb_communication_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_communication
    ADD CONSTRAINT tb_communication_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;
