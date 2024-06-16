-- drop existing extensions
DROP EXTENSION IF EXISTS "uuid-ossp" CASCADE;

-- drop existing tables
DROP TABLE IF EXISTS tb_user CASCADE;
DROP TABLE IF EXISTS tb_event CASCADE;
DROP TABLE IF EXISTS tb_address CASCADE;
DROP TABLE IF EXISTS tb_profile CASCADE;
DROP TABLE IF EXISTS tb_group CASCADE;
DROP TABLE IF EXISTS tb_participant CASCADE;
DROP TABLE IF EXISTS tb_participant_group CASCADE;

-- uuid-ossp definition
CREATE EXTENSION "uuid-ossp";

-- tb_ user definition
CREATE TABLE tb_user
(
    id                 uuid      NOT NULL DEFAULT uuid_generate_v4(),
    oidc_id            uuid      NOT NULL,
    first_name         VARCHAR(50),
    last_name          VARCHAR(50),
    email              VARCHAR(320),
    role               VARCHAR,
    default_profile_id uuid,
    create_date        TIMESTAMP NOT NULL,
    create_user_id     uuid,
    edit_date          TIMESTAMP NOT NULL,
    edit_user_id       uuid,
    visible            BOOLEAN   NOT NULL,
    CONSTRAINT tb_user_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX tb_user_oidc_id ON tb_user (oidc_id);
CREATE INDEX tb_user_email ON tb_user (email);

-- tb_event definition
CREATE TABLE tb_event
(
    id             uuid         NOT NULL DEFAULT uuid_generate_v4(),
    name           VARCHAR(150) NOT NULL,
    address_id     uuid,
    start_time     TIMESTAMP,
    end_time       TIMESTAMP,
    options        TEXT[]       NOT NULL,
    parent_id      uuid,
    create_date    TIMESTAMP    NOT NULL,
    create_user_id uuid,
    edit_date      TIMESTAMP    NOT NULL,
    edit_user_id   uuid,
    visible        BOOLEAN      NOT NULL,
    CONSTRAINT tb_event_pkey PRIMARY KEY (id)
);

-- tb_address definition
CREATE TABLE tb_address
(
    id                        uuid        NOT NULL DEFAULT uuid_generate_v4(),
    number                    VARCHAR(15),
    street                    VARCHAR     NOT NULL,
    complementary_information VARCHAR,
    zip_code                  VARCHAR(15),
    city                      VARCHAR(50) NOT NULL,
    country                   VARCHAR(50) NOT NULL,
    create_date               TIMESTAMP   NOT NULL,
    create_user_id            uuid,
    edit_date                 TIMESTAMP   NOT NULL,
    edit_user_id              uuid,
    visible                   BOOLEAN     NOT NULL,
    CONSTRAINT tb_address_pkey PRIMARY KEY (id)
);

-- tb_profile definition
CREATE TABLE tb_profile
(
    id             uuid      NOT NULL DEFAULT uuid_generate_v4(),
    user_id        uuid      NOT NULL,
    event_id       uuid      NOT NULL,
    role           VARCHAR   NOT NULL,
    accepted       BOOLEAN   NOT NULL,
    start_access   TIMESTAMP,
    end_access     TIMESTAMP,
    create_date    TIMESTAMP NOT NULL,
    create_user_id uuid,
    edit_date      TIMESTAMP NOT NULL,
    edit_user_id   uuid,
    visible        BOOLEAN   NOT NULL,
    CONSTRAINT tb_profile_pkey PRIMARY KEY (id)
);

-- tb_group definition
CREATE TABLE tb_group
(
    id             uuid         NOT NULL DEFAULT uuid_generate_v4(),
    name           VARCHAR(150) NOT NULL,
    start_time     TIMESTAMP,
    end_time       TIMESTAMP,
    parent_id      uuid         NOT NULL,
    event_id       uuid         NOT NULL,
    create_date    TIMESTAMP    NOT NULL,
    create_user_id uuid,
    edit_date      TIMESTAMP    NOT NULL,
    edit_user_id   uuid,
    visible        BOOLEAN      NOT NULL,
    CONSTRAINT tb_group_pkey PRIMARY KEY (id)
);

-- tb_participant definition
CREATE TABLE tb_participant
(
    id             uuid         NOT NULL DEFAULT uuid_generate_v4(),
    first_name     VARCHAR(150) NOT NULL,
    last_name      VARCHAR(150) NOT NULL,
    birth_day      DATE         NOT NULL,
    is_driver      BOOLEAN      NOT NULL,
    start_time     TIMESTAMP,
    end_time       TIMESTAMP,
    event_id       uuid         NOT NULL,
    create_date    TIMESTAMP    NOT NULL,
    create_user_id uuid,
    edit_date      TIMESTAMP    NOT NULL,
    edit_user_id   uuid,
    visible        BOOLEAN      NOT NULL,
    CONSTRAINT tb_participant_pkey PRIMARY KEY (id)
);

-- tb_participant_group definition
CREATE TABLE tb_participant_group
(
    participant_id uuid NOT NULL,
    group_id       uuid NOT NULL,
    CONSTRAINT tb_participant_group_pkey PRIMARY KEY (participant_id, group_id)
);

-- tb_user foreign keys
ALTER TABLE tb_user
    ADD CONSTRAINT tb_user_default_profile_fkey FOREIGN KEY (default_profile_id) REFERENCES tb_profile (id) ON DELETE SET NULL;
ALTER TABLE tb_user
    ADD CONSTRAINT tb_user_create_user_fkey FOREIGN KEY (create_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_user
    ADD CONSTRAINT tb_user_edit_user_fkey FOREIGN KEY (edit_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_event foreign keys
ALTER TABLE tb_event
    ADD CONSTRAINT tb_event_address_fkey FOREIGN KEY (address_id) REFERENCES tb_address (id) ON DELETE SET NULL;
ALTER TABLE tb_event
    ADD CONSTRAINT tb_event_parent_fkey FOREIGN KEY (parent_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_event
    ADD CONSTRAINT tb_event_create_user_fkey FOREIGN KEY (create_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_event
    ADD CONSTRAINT tb_event_edit_user_fkey FOREIGN KEY (edit_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_address foreign keys
ALTER TABLE tb_address
    ADD CONSTRAINT tb_address_create_user_fkey FOREIGN KEY (create_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_address
    ADD CONSTRAINT tb_address_edit_user_fkey FOREIGN KEY (edit_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_profile foreign keys
ALTER TABLE tb_profile
    ADD CONSTRAINT tb_profile_user_fkey FOREIGN KEY (user_id) REFERENCES tb_user (id) ON DELETE CASCADE;
ALTER TABLE tb_profile
    ADD CONSTRAINT tb_profile_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_profile
    ADD CONSTRAINT tb_profile_create_user_fkey FOREIGN KEY (create_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_profile
    ADD CONSTRAINT tb_profile_edit_user_fkey FOREIGN KEY (edit_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_group foreign keys
ALTER TABLE tb_group
    ADD CONSTRAINT tb_group_parent_fkey FOREIGN KEY (parent_id) REFERENCES tb_group (id) ON DELETE CASCADE;
ALTER TABLE tb_group
    ADD CONSTRAINT tb_group_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_group
    ADD CONSTRAINT tb_group_create_user_fkey FOREIGN KEY (create_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_group
    ADD CONSTRAINT tb_group_edit_user_fkey FOREIGN KEY (edit_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_participant foreign keys
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_event_fkey FOREIGN KEY (event_id) REFERENCES tb_event (id) ON DELETE CASCADE;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_create_user_fkey FOREIGN KEY (create_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;
ALTER TABLE tb_participant
    ADD CONSTRAINT tb_participant_edit_user_fkey FOREIGN KEY (edit_user_id) REFERENCES tb_user (id) ON DELETE SET NULL;

-- tb_participant_group foreign keys
ALTER TABLE tb_participant_group
    ADD CONSTRAINT tb_participant_group_group_fkey FOREIGN KEY (group_id) REFERENCES tb_group (id) ON DELETE CASCADE;
ALTER TABLE tb_participant_group
    ADD CONSTRAINT tb_participant_group_participant_fkey FOREIGN KEY (participant_id) REFERENCES tb_user (id) ON DELETE CASCADE;

-- tb_user insert service user
INSERT INTO tb_user(id, oidc_id, create_date, create_user_id, edit_date, edit_user_id, visible)
VALUES ('3ca811bf-1677-4008-ac11-6e767c44d3c1', uuid_generate_v4(), '1998-10-13 00:00:00.000',
        '3ca811bf-1677-4008-ac11-6e767c44d3c1',
        '1998-10-13 00:00:00.000', '3ca811bf-1677-4008-ac11-6e767c44d3c1', TRUE)
