-- tb_group definition
CREATE TABLE tb_group
(
    id                      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name                    VARCHAR(150)             NOT NULL,
    start_availability_date DATE,
    start_availability_time TIME WITH TIME ZONE,
    end_availability_date   DATE,
    end_availability_time   TIME WITH TIME ZONE,
    event_id                uuid                     NOT NULL,
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              uuid,
    last_modified_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by        uuid,
    visible                 BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_group_pkey PRIMARY KEY (id)
);

-- tb_group_content definition
CREATE TABLE tb_group_content
(
    id             uuid NOT NULL DEFAULT uuid_generate_v4(),
    group_id       uuid NOT NULL,
    participant_id uuid NOT NULL,
    CONSTRAINT tb_group_content_pkey PRIMARY KEY (group_id, participant_id)
);

-- tb_movement_content adding pool_name
ALTER TABLE tb_movement_content
    ADD COLUMN pool_name VARCHAR;

-- tb_group foreign keys and indexes
CREATE INDEX tb_group_index_event_id ON tb_group (event_id);
CREATE INDEX tb_group_index_created_by ON tb_group (created_by);
CREATE INDEX tb_group_index_last_modified_by ON tb_group (last_modified_by);

ALTER TABLE tb_group
    ADD CONSTRAINT tb_group_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_group
    ADD CONSTRAINT tb_group_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_group
    ADD CONSTRAINT tb_group_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_group_content foreign keys and indexes
CREATE UNIQUE INDEX tb_group_content_index_group_and_participant_id ON tb_group_content (group_id, participant_id);

ALTER TABLE tb_group_content
    ADD CONSTRAINT tb_group_content_group_fkey FOREIGN KEY (group_id) REFERENCES tb_group (id) ON DELETE CASCADE;
ALTER TABLE tb_group_content
    ADD CONSTRAINT tb_group_content_participant_fkey FOREIGN KEY (participant_id) REFERENCES tb_participant (id) ON DELETE CASCADE;
