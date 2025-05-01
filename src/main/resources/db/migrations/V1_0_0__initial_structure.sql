-- extension definition
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS unaccent;

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

-- tb_project_permission definition
CREATE TABLE tb_project_permission
(
    name VARCHAR NOT NULL,
    CONSTRAINT tb_project_permission_pkey PRIMARY KEY (name)
);

-- tb_project_role definition
CREATE TABLE tb_project_role
(
    name  VARCHAR NOT NULL,
    level INT     NOT NULL,
    CONSTRAINT tb_project_role_pkey PRIMARY KEY (name)
);

-- tb_project_role_permission definition
CREATE TABLE tb_project_role_permission
(
    id         uuid    NOT NULL DEFAULT uuid_generate_v4(),
    role       VARCHAR NOT NULL,
    permission VARCHAR NOT NULL,
    CONSTRAINT tb_project_role_permission_pkey PRIMARY KEY (id)
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

-- tb_project definition
CREATE TABLE tb_project
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    name               VARCHAR(150)             NOT NULL,
    begin_date         DATE,
    begin_time         TIME WITH TIME ZONE,
    end_date           DATE,
    end_time           TIME WITH TIME ZONE,
    options            TEXT[]                   NOT NULL DEFAULT '{}',
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_project_pkey PRIMARY KEY (id)
);

-- tb_project_profile definition
CREATE TABLE tb_project_profile
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    user_id            uuid                     NOT NULL,
    project_id         uuid                     NOT NULL,
    role               VARCHAR                  NOT NULL,
    status             VARCHAR                  NOT NULL,
    start_access_date  DATE,
    start_access_time  TIME WITH TIME ZONE,
    end_access_date    DATE,
    end_access_time    TIME WITH TIME ZONE,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         uuid,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   uuid,
    visible            BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_project_profile_pkey PRIMARY KEY (id)
);

-- tb_participant definition
CREATE TABLE tb_participant
(
    id                      uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    first_name              VARCHAR(150)             NOT NULL,
    last_name               VARCHAR(150)             NOT NULL,
    birthday                DATE                     NOT NULL,
    start_availability_date DATE,
    start_availability_time TIME WITH TIME ZONE,
    end_availability_date   DATE,
    end_availability_time   TIME WITH TIME ZONE,
    user_id                 uuid,
    project_id              uuid                     NOT NULL,
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              uuid,
    last_modified_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by        uuid,
    purged                  BOOLEAN                  NOT NULL DEFAULT FALSE,
    visible                 BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT tb_participant_pkey PRIMARY KEY (id)
);

-- tb_movement definition
CREATE TABLE tb_movement
(
    id                 uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    date_time          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type               VARCHAR                  NOT NULL,
    project_id         uuid                     NOT NULL,
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

-- tb_project_role foreign keys and indexes
CREATE UNIQUE INDEX tb_project_role_level0 ON tb_project_role (level) WHERE level = 0;
CREATE UNIQUE INDEX tb_project_role_index_role ON tb_project_role (name);

-- tb_user_role_permission foreign keys and indexes
CREATE UNIQUE INDEX tb_user_role_permission_unique ON tb_user_role_permission (role, permission);

ALTER TABLE tb_user_role_permission
    ADD CONSTRAINT tb_user_role_permission_role_fkey FOREIGN KEY (role) REFERENCES tb_user_role (name) ON DELETE CASCADE;
ALTER TABLE tb_user_role_permission
    ADD CONSTRAINT tb_user_role_permission_permission_fkey FOREIGN KEY (permission) REFERENCES tb_user_permission (name) ON DELETE CASCADE;

-- tb_project_role_permission foreign keys and indexes
CREATE UNIQUE INDEX tb_project_role_permission_unique ON tb_project_role_permission (role, permission);

ALTER TABLE tb_project_role_permission
    ADD CONSTRAINT tb_project_role_permission_role_fkey FOREIGN KEY (role) REFERENCES tb_project_role (name) ON DELETE CASCADE;
ALTER TABLE tb_project_role_permission
    ADD CONSTRAINT tb_project_role_permission_permission_fkey FOREIGN KEY (permission) REFERENCES tb_project_permission (name) ON DELETE CASCADE;

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
    ADD CONSTRAINT tb_preferences_default_profile_fkey FOREIGN KEY (selected_profile_id) REFERENCES tb_project_profile (id) ON DELETE SET NULL;
ALTER TABLE tb_preferences
    ADD CONSTRAINT tb_preferences_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_preferences
    ADD CONSTRAINT tb_preferences_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_project foreign keys and indexes
CREATE INDEX tb_project_index_created_by ON tb_project (created_by);
CREATE INDEX tb_project_index_last_modified_by ON tb_project (last_modified_by);

ALTER TABLE tb_project
    ADD CONSTRAINT tb_project_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_project
    ADD CONSTRAINT tb_project_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_project_profile foreign keys and indexes
CREATE INDEX tb_project_profile_index_user_id ON tb_project_profile (user_id);
CREATE INDEX tb_project_profile_index_project_id ON tb_project_profile (project_id);
CREATE INDEX tb_project_profile_index_created_by ON tb_project_profile (created_by);
CREATE INDEX tb_project_profile_index_last_modified_by ON tb_project_profile (last_modified_by);

ALTER TABLE tb_project_profile
    ADD CONSTRAINT tb_project_profile_role_fkey FOREIGN KEY (role) REFERENCES tb_project_role (name) ON DELETE CASCADE;
ALTER TABLE tb_project_profile
    ADD CONSTRAINT tb_project_profile_user_fkey FOREIGN KEY (user_id) REFERENCES tb_user (id) ON DELETE CASCADE;
ALTER TABLE tb_project_profile
    ADD CONSTRAINT tb_project_profile_project_fkey FOREIGN KEY (project_id) REFERENCES tb_project (id) ON DELETE CASCADE;
ALTER TABLE tb_project_profile
    ADD CONSTRAINT tb_project_profile_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_project_profile
    ADD CONSTRAINT tb_project_profile_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_participant foreign keys and indexes
CREATE INDEX tb_participant_index_user_id ON tb_participant (user_id);
CREATE INDEX tb_participant_index_project_id ON tb_participant (project_id);
CREATE INDEX tb_participant_index_created_by ON tb_participant (created_by);
CREATE INDEX tb_participant_index_last_modified_by ON tb_participant (last_modified_by);

ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_user_fkey FOREIGN KEY (user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_project_fkey FOREIGN KEY (project_id) REFERENCES tb_project (id) ON DELETE CASCADE;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_create_user_fkey FOREIGN KEY (created_by) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_edit_user_fkey FOREIGN KEY (last_modified_by) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_movement foreign keys and indexes
CREATE INDEX tb_movement_index_project_id ON tb_movement (project_id);
CREATE INDEX tb_movement_index_created_by ON tb_movement (created_by);
CREATE INDEX tb_movement_index_last_modified_by ON tb_movement (last_modified_by);

ALTER TABLE tb_movement
    ADD CONSTRAINT tb_movement_project_fkey FOREIGN KEY (project_id) REFERENCES tb_project (id) ON DELETE CASCADE;
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
