-- tb_activity definition
CREATE TABLE tb_activity
(
    id                       uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name                     VARCHAR(150)             NOT NULL,
    description              VARCHAR(2000),
    duration                 VARCHAR(10),
    min_allowed_participants INT,
    max_allowed_participants INT,
    start_availability_date  DATE,
    start_availability_time  TIME WITH TIME ZONE,
    end_availability_date    DATE,
    end_availability_time    TIME WITH TIME ZONE,
    event_id                 uuid                     NOT NULL,
    created_date             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               uuid,
    last_modified_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by         uuid,
    visible                  BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_activity_pkey PRIMARY KEY (id)
);

-- tb_movement_content adding pool_name
ALTER TABLE tb_movement
    ADD COLUMN activity_id uuid;

-- tb_activity foreign keys and indexes
CREATE INDEX tb_activity_index_event_id ON tb_vehicle (event_id);
CREATE INDEX tb_activity_index_created_by ON tb_vehicle (created_by);
CREATE INDEX tb_activity_index_last_modified_by ON tb_vehicle (last_modified_by);

ALTER TABLE tb_activity
    ADD CONSTRAINT tb_activity_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_activity
    ADD CONSTRAINT tb_activity_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_activity
    ADD CONSTRAINT tb_activity_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_movement foreign keys and indexes
ALTER TABLE tb_movement
    ADD CONSTRAINT tb_movement_activity_fkey FOREIGN KEY (activity_id) REFERENCES tb_activity (id) ON DELETE CASCADE;;
