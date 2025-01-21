-- drop existing extensions
DROP EXTENSION IF EXISTS "uuid-ossp" CASCADE;

-- drop existing tables
DROP TABLE IF EXISTS tb_user CASCADE;
DROP TABLE IF EXISTS tb_preferences CASCADE;
DROP TABLE IF EXISTS tb_event CASCADE;
DROP TABLE IF EXISTS tb_event_profile CASCADE;
DROP TABLE IF EXISTS tb_participant CASCADE;
DROP TABLE IF EXISTS tb_group CASCADE;
DROP TABLE IF EXISTS tb_group_content CASCADE;
DROP TABLE IF EXISTS tb_movement CASCADE;
DROP TABLE IF EXISTS tb_movement_content CASCADE;

-- uuid-ossp definition
CREATE EXTENSION "uuid-ossp";

-- tb_user_permission definition
CREATE TABLE tb_user_permission
(
    name VARCHAR NOT NULL,
    CONSTRAINT tb_user_permission_pkey PRIMARY KEY (name)
);

-- tb_user_role definition
CREATE TABLE tb_user_role
(
    name  VARCHAR NOT NULL,
    level INT     NOT NULL,
    CONSTRAINT tb_user_role_pkey PRIMARY KEY (name)
);

-- tb_user_role_permission definition
CREATE TABLE tb_user_role_permission
(
    id         uuid    NOT NULL DEFAULT uuid_generate_v4(),
    role       VARCHAR NOT NULL,
    permission VARCHAR NOT NULL,
    CONSTRAINT tb_user_role_permission_pkey PRIMARY KEY (id)
);

-- tb_event_permission definition
CREATE TABLE tb_event_permission
(
    name VARCHAR NOT NULL,
    CONSTRAINT tb_event_permission_pkey PRIMARY KEY (name)
);

-- tb_event_role definition
CREATE TABLE tb_event_role
(
    name  VARCHAR NOT NULL,
    level INT     NOT NULL,
    CONSTRAINT tb_event_role_pkey PRIMARY KEY (name)
);

-- tb_event_role_permission definition
CREATE TABLE tb_event_role_permission
(
    id         uuid    NOT NULL DEFAULT uuid_generate_v4(),
    role       VARCHAR NOT NULL,
    permission VARCHAR NOT NULL,
    CONSTRAINT tb_event_role_permission_pkey PRIMARY KEY (id)
);

-- tb_user definition
CREATE TABLE tb_user
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    oidc_id            uuid,
    type               VARCHAR                  NOT NULL DEFAULT 'USER',
    first_name         VARCHAR(50),
    last_name          VARCHAR(50),
    email              VARCHAR(320),
    role               VARCHAR,
    birthday           DATE,
    last_login         TIMESTAMP WITH TIME ZONE,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    purged             BOOLEAN                  NOT NULL DEFAULT FALSE,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_user_pkey PRIMARY KEY (id)
);

-- tb_preferences definition
CREATE TABLE tb_preferences
(
    id                  uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    user_id             uuid                     NOT NULL,
    selected_profile_id uuid,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          uuid,
    last_modified_date  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by    uuid,
    visible             BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_preferences_pkey PRIMARY KEY (id)
);

-- tb_event definition
CREATE TABLE tb_event
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name               VARCHAR(150)             NOT NULL,
    begin              TIMESTAMP WITH TIME ZONE,
    finish             TIMESTAMP WITH TIME ZONE,
    options            TEXT[]                   NOT NULL DEFAULT '{}',
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_event_pkey PRIMARY KEY (id)
);

-- tb_event_profile definition
CREATE TABLE tb_event_profile
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    user_id            uuid                     NOT NULL,
    event_id           uuid                     NOT NULL,
    role               VARCHAR                  NOT NULL,
    status             VARCHAR                  NOT NULL,
    start_access       TIMESTAMP WITH TIME ZONE,
    end_access         TIMESTAMP WITH TIME ZONE,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_event_profile_pkey PRIMARY KEY (id)
);

-- tb_participant definition
CREATE TABLE tb_participant
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    first_name         VARCHAR(150)             NOT NULL,
    last_name          VARCHAR(150)             NOT NULL,
    birthday           DATE                     NOT NULL,
    begin              TIMESTAMP WITH TIME ZONE,
    finish             TIMESTAMP WITH TIME ZONE,
    user_id            uuid,
    event_id           uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    purged             BOOLEAN                  NOT NULL DEFAULT FALSE,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_participant_pkey PRIMARY KEY (id)
);

-- tb_group definition
CREATE TABLE tb_group
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name               VARCHAR(150)             NOT NULL,
    begin              TIMESTAMP WITH TIME ZONE,
    finish             TIMESTAMP WITH TIME ZONE,
    event_id           uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
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

-- tb_movement definition
CREATE TABLE tb_movement
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    date_time          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type               VARCHAR                  NOT NULL,
    event_id           uuid                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_movement_pkey PRIMARY KEY (id)
);

-- tb_movement_content definition
CREATE TABLE tb_movement_content
(
    id             uuid NOT NULL DEFAULT uuid_generate_v4(),
    movement_id    uuid NOT NULL,
    participant_id uuid NOT NULL,
    CONSTRAINT tb_movement_content_pkey PRIMARY KEY (movement_id, participant_id)
);

-- tb_user_role foreign keys and indexes
CREATE UNIQUE INDEX tb_user_role_level0 ON tb_user_role (level) WHERE level = 0;
CREATE UNIQUE INDEX tb_user_role_index_role ON tb_user_role (name);

-- tb_event_role foreign keys and indexes
CREATE UNIQUE INDEX tb_event_role_level0 ON tb_event_role (level) WHERE level = 0;
CREATE UNIQUE INDEX tb_event_role_index_role ON tb_event_role (name);

-- tb_user_role_permission foreign keys and indexes
CREATE UNIQUE INDEX tb_user_role_permission_unique ON tb_user_role_permission (role, permission);

ALTER TABLE tb_user_role_permission
    ADD CONSTRAINT tb_user_role_permission_role_fkey FOREIGN KEY (role) REFERENCES tb_user_role (name) ON DELETE CASCADE;
ALTER TABLE tb_user_role_permission
    ADD CONSTRAINT tb_user_role_permission_permission_fkey FOREIGN KEY (permission) REFERENCES tb_user_permission (name) ON DELETE CASCADE;

-- tb_event_role_permission foreign keys and indexes
CREATE UNIQUE INDEX tb_event_role_permission_unique ON tb_event_role_permission (role, permission);

ALTER TABLE tb_event_role_permission
    ADD CONSTRAINT tb_event_role_permission_role_fkey FOREIGN KEY (role) REFERENCES tb_event_role (name) ON DELETE CASCADE;
ALTER TABLE tb_event_role_permission
    ADD CONSTRAINT tb_event_role_permission_permission_fkey FOREIGN KEY (permission) REFERENCES tb_event_permission (name) ON DELETE CASCADE;

-- tb_user foreign keys and indexes
CREATE UNIQUE INDEX tb_user_index_oidc_id ON tb_user (oidc_id);
CREATE UNIQUE INDEX tb_user_index_email ON tb_user (email);
CREATE UNIQUE INDEX tb_user_service_account ON tb_user (type) WHERE type = 'SERVICE_ACCOUNT';
CREATE INDEX tb_user_index_created_by ON tb_user (created_by);
CREATE INDEX tb_user_index_last_modified_by ON tb_user (last_modified_by);

ALTER TABLE tb_user
    ADD CONSTRAINT tb_user_role_fkey FOREIGN KEY (role) REFERENCES tb_user_role (name) ON DELETE SET NULL;
ALTER TABLE tb_user
    ADD CONSTRAINT tb_user_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_user
    ADD CONSTRAINT tb_user_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_preferences foreign keys
CREATE UNIQUE INDEX tb_preferences_index_user_id ON tb_preferences (user_id);
CREATE INDEX tb_preferences_index_selected_profile_id ON tb_preferences (selected_profile_id);
CREATE INDEX tb_preferences_index_created_by ON tb_preferences (created_by);
CREATE INDEX tb_preferences_index_last_modified_by ON tb_preferences (last_modified_by);

ALTER TABLE tb_preferences
    ADD CONSTRAINT tb_preferences_user_fkey FOREIGN KEY (user_id) REFERENCES tb_user (id) ON DELETE CASCADE;
ALTER TABLE tb_preferences
    ADD CONSTRAINT tb_preferences_default_profile_fkey FOREIGN KEY (selected_profile_id) REFERENCES tb_event_profile (id) ON DELETE SET NULL;
ALTER TABLE tb_preferences
    ADD CONSTRAINT tb_preferences_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_preferences
    ADD CONSTRAINT tb_preferences_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_event foreign keys and indexes
CREATE INDEX tb_event_index_created_by ON tb_event (created_by);
CREATE INDEX tb_event_index_last_modified_by ON tb_event (last_modified_by);

ALTER TABLE tb_event
    ADD CONSTRAINT tb_event_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_event
    ADD CONSTRAINT tb_event_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_event_profile foreign keys and indexes
CREATE INDEX tb_event_profile_index_user_id ON tb_event_profile (user_id);
CREATE INDEX tb_event_profile_index_event_id ON tb_event_profile (event_id);
CREATE INDEX tb_event_profile_index_created_by ON tb_event_profile (created_by);
CREATE INDEX tb_event_profile_index_last_modified_by ON tb_event_profile (last_modified_by);

ALTER TABLE tb_event_profile
    ADD CONSTRAINT tb_event_profile_role_fkey FOREIGN KEY (role) REFERENCES tb_event_role (name) ON DELETE CASCADE;
ALTER TABLE tb_event_profile
    ADD CONSTRAINT tb_event_profile_user_fkey FOREIGN KEY (user_id) REFERENCES tb_user (id) ON DELETE CASCADE;
ALTER TABLE tb_event_profile
    ADD CONSTRAINT tb_event_profile_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_event_profile
    ADD CONSTRAINT tb_event_profile_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_event_profile
    ADD CONSTRAINT tb_event_profile_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_participant foreign keys and indexes
CREATE INDEX tb_participant_index_user_id ON tb_participant (user_id);
CREATE INDEX tb_participant_index_event_id ON tb_participant (event_id);
CREATE INDEX tb_participant_index_created_by ON tb_participant (created_by);
CREATE INDEX tb_participant_index_last_modified_by ON tb_participant (last_modified_by);

ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_user_fkey FOREIGN KEY (user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

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

-- tb_movement foreign keys and indexes
CREATE INDEX tb_movement_index_event_id ON tb_movement (event_id);
CREATE INDEX tb_movement_index_created_by ON tb_movement (created_by);
CREATE INDEX tb_movement_index_last_modified_by ON tb_movement (last_modified_by);

ALTER TABLE tb_movement
    ADD CONSTRAINT tb_movement_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_movement
    ADD CONSTRAINT tb_movement_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_movement
    ADD CONSTRAINT tb_movement_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_movement_content foreign keys and indexes
CREATE UNIQUE INDEX tb_movement_content_index_movement_and_participant_id ON tb_movement_content (movement_id, participant_id);

ALTER TABLE tb_movement_content
    ADD CONSTRAINT tb_movement_content_movement_fkey FOREIGN KEY (movement_id) REFERENCES tb_movement (id) ON DELETE CASCADE;
ALTER TABLE tb_movement_content
    ADD CONSTRAINT tb_movement_content_participant_fkey FOREIGN KEY (participant_id) REFERENCES tb_participant (id) ON DELETE CASCADE;

-- tb_user insert service user
INSERT INTO tb_user(type)
VALUES ('SERVICE_ACCOUNT');

-- tb_user_permission insert service user
INSERT INTO tb_user_permission(name)
VALUES ('REGISTRY_USER_R'),
       ('REGISTRY_USER_U'),
       ('REGISTRY_USER_D'),
       ('REGISTRY_USER_METADATA_R'),
       ('REGISTRY_EVENT_C'),
       ('REGISTRY_EVENT_R'),
       ('REGISTRY_EVENT_METADATA_R'),
       ('REGISTRY_PROFILE_C')
;

-- tb_event_permission insert service user
INSERT INTO tb_event_permission(name)
VALUES ('REGISTRY_EVENT_R'),
       ('REGISTRY_EVENT_U'),
       ('REGISTRY_EVENT_D'),
       ('REGISTRY_EVENT_PROFILE_C'),
       ('REGISTRY_EVENT_PROFILE_R'),
       ('REGISTRY_EVENT_PROFILE_U'),
       ('REGISTRY_EVENT_PROFILE_D'),
       ('REGISTRY_EVENT_PROFILE_METADATA_R'),
       ('REGISTRY_EVENT_PARTICIPANT_C'),
       ('REGISTRY_EVENT_PARTICIPANT_R'),
       ('REGISTRY_EVENT_PARTICIPANT_U'),
       ('REGISTRY_EVENT_PARTICIPANT_D'),
       ('REGISTRY_EVENT_PARTICIPANT_METADATA_R'),
       ('REGISTRY_EVENT_GROUP_C'),
       ('REGISTRY_EVENT_GROUP_R'),
       ('REGISTRY_EVENT_GROUP_U'),
       ('REGISTRY_EVENT_GROUP_D'),
       ('REGISTRY_EVENT_GROUP_METADATA_R'),
       ('REGISTRY_EVENT_MOVEMENT_C'),
       ('REGISTRY_EVENT_MOVEMENT_R'),
       ('REGISTRY_EVENT_MOVEMENT_U'),
       ('REGISTRY_EVENT_MOVEMENT_D'),
       ('REGISTRY_EVENT_MOVEMENT_METADATA_R')
;
