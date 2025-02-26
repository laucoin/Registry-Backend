-- tb_vehicle definition
CREATE TABLE tb_vehicle
(
    id                      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    license_plate           VARCHAR(20)              NOT NULL,
    brand                   VARCHAR(150)             NOT NULL,
    model                   VARCHAR(150)             NOT NULL,
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
    CONSTRAINT tb_vehicle_pkey PRIMARY KEY (id)
);

-- tb_movement_content adding pool_name
ALTER TABLE tb_movement_content
    ADD COLUMN vehicle_id uuid;

-- tb_vehicle foreign keys and indexes
CREATE INDEX tb_vehicle_index_event_id ON tb_vehicle (event_id);
CREATE INDEX tb_vehicle_index_created_by ON tb_vehicle (created_by);
CREATE INDEX tb_vehicle_index_last_modified_by ON tb_vehicle (last_modified_by);

ALTER TABLE tb_vehicle
    ADD CONSTRAINT tb_vehicle_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_vehicle
    ADD CONSTRAINT tb_vehicle_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_vehicle
    ADD CONSTRAINT tb_vehicle_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_movement_content foreign keys and indexes
CREATE UNIQUE INDEX tb_movement_content_index_movement_and_vehicle_id ON tb_movement_content (movement_id, vehicle_id);

ALTER TABLE tb_movement_content
    ADD CONSTRAINT tb_movement_content_vehicle_fkey FOREIGN KEY (vehicle_id) REFERENCES tb_vehicle (id) ON DELETE CASCADE;
