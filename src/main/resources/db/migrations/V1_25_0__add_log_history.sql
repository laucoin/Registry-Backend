-- tb_alert_history definition
CREATE TABLE tb_alert_history
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    alert_id           uuid                     NOT NULL,
    version            INTEGER                  NOT NULL,
    archived_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_time          TIMESTAMP WITH TIME ZONE NOT NULL,
    title              VARCHAR(50),
    status             VARCHAR(50),
    project_id         uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL,
    CONSTRAINT tb_alert_history_pkey PRIMARY KEY (id),
    CONSTRAINT tb_alert_history_version_unique UNIQUE (alert_id, version),
    CONSTRAINT tb_alert_history_alert_fkey FOREIGN KEY (alert_id) REFERENCES tb_alert (id) ON DELETE CASCADE
);

-- tb_communication_history definition
CREATE TABLE tb_communication_history
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    communication_id   uuid                     NOT NULL,
    version            INTEGER                  NOT NULL,
    archived_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_time          TIMESTAMP WITH TIME ZONE NOT NULL,
    movement_id        uuid,
    alert_id           uuid,
    message            VARCHAR(250),
    project_id         uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL,
    CONSTRAINT tb_communication_history_pkey PRIMARY KEY (id),
    CONSTRAINT tb_communication_history_version_unique UNIQUE (communication_id, version),
    CONSTRAINT tb_communication_history_communication_fkey FOREIGN KEY (communication_id) REFERENCES tb_communication (id) ON DELETE CASCADE
);

-- tb_movement_history definition
CREATE TABLE tb_movement_history
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    movement_id        uuid                     NOT NULL,
    version            INTEGER                  NOT NULL,
    archived_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_time          TIMESTAMP WITH TIME ZONE NOT NULL,
    type               VARCHAR                  NOT NULL,
    reason             VARCHAR(50),
    activity_id        uuid,
    project_id         uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL,
    CONSTRAINT tb_movement_history_pkey PRIMARY KEY (id),
    CONSTRAINT tb_movement_history_version_unique UNIQUE (movement_id, version),
    CONSTRAINT tb_movement_history_movement_fkey FOREIGN KEY (movement_id) REFERENCES tb_movement (id) ON DELETE CASCADE
);

-- tb_movement_content_history definition
CREATE TABLE tb_movement_content_history
(
    id                  uuid NOT NULL DEFAULT uuid_generate_v4(),
    movement_history_id uuid NOT NULL,
    participant_id      uuid,
    vehicle_id          uuid,
    pool_name           VARCHAR,
    CONSTRAINT tb_movement_content_history_pkey PRIMARY KEY (id),
    CONSTRAINT tb_movement_content_history_history_fkey FOREIGN KEY (movement_history_id) REFERENCES tb_movement_history (id) ON DELETE CASCADE
);

CREATE INDEX tb_movement_content_history_index_history_id ON tb_movement_content_history (movement_history_id);
