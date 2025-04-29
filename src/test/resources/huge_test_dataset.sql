-- Insert data into tb_user
INSERT INTO tb_user (id, oidc_id, type, first_name, last_name, email, role, birthday, last_login, created_by,
                     last_modified_by)
VALUES ('9cd10ea7-96c1-4f82-8366-d11d2e3ec300', '08513d7b-cce1-4efe-bb27-8d936c4a12b4', 'USER', 'Jane', 'SMITH',
        'administrator@sgdf.fr', 'USER_ADMINISTRATOR', '1980-01-01', '2025-03-01 15:28:51.144372 +00:00',
        '9cd10ea7-96c1-4f82-8366-d11d2e3ec300', '9cd10ea7-96c1-4f82-8366-d11d2e3ec300'),
       ('fd705f30-3cbd-478d-ab37-107724321dca', 'c1a62b72-cbc4-4c34-a40a-8a574ff59da5', 'USER', 'John', 'DOE',
        'coordinator@sgdf.fr', 'USER', '1990-01-01', '2025-03-01 15:28:51.144372 +00:00',
        'fd705f30-3cbd-478d-ab37-107724321dca', 'fd705f30-3cbd-478d-ab37-107724321dca'),
       ('e22a08da-b8b8-4b78-86c8-8557ddfbb945', '5d4a06a8-5c23-4795-b534-446f706af099', 'USER', 'Charles', 'PINA',
        'participant@sgdf.fr', 'USER', '2000-01-01', '2025-03-01 15:28:51.144372 +00:00',
        'e22a08da-b8b8-4b78-86c8-8557ddfbb945', 'e22a08da-b8b8-4b78-86c8-8557ddfbb945');

-- Insert data into tb_preferences
INSERT INTO tb_preferences (user_id, created_by, last_modified_by)
VALUES ('9cd10ea7-96c1-4f82-8366-d11d2e3ec300', '9cd10ea7-96c1-4f82-8366-d11d2e3ec300',
        '9cd10ea7-96c1-4f82-8366-d11d2e3ec300'),
       ('fd705f30-3cbd-478d-ab37-107724321dca', 'fd705f30-3cbd-478d-ab37-107724321dca',
        'fd705f30-3cbd-478d-ab37-107724321dca'),
       ('e22a08da-b8b8-4b78-86c8-8557ddfbb945', 'e22a08da-b8b8-4b78-86c8-8557ddfbb945',
        'e22a08da-b8b8-4b78-86c8-8557ddfbb945');

-- Insert data into tb_project
INSERT INTO tb_project (name, begin_date, begin_time, end_date, end_time, created_by, last_modified_by)
SELECT 'Project ' || i,
       NOW() - (RANDOM() * INTERVAL '30 days'),
       NOW() - (RANDOM() * INTERVAL '30 minutes'),
       NOW() + (RANDOM() * INTERVAL '30 days'),
       NOW() + (RANDOM() * INTERVAL '30 minutes'),
       (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1),
       (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1)
FROM GENERATE_SERIES(1, 15) AS i;

-- Insert data into tb_project_profile
DO
$$
    DECLARE
        project_id         uuid;
        project_begin_date DATE;
        project_begin_time TIME WITH TIME ZONE;
        project_end_date   DATE;
        project_end_time   TIME WITH TIME ZONE;
        user_id            uuid;
        begin_seconds      DOUBLE PRECISION;
        end_seconds        DOUBLE PRECISION;
    BEGIN
        FOR project_id IN SELECT id FROM tb_project
            LOOP
                SELECT begin_date, begin_time, end_date, end_time
                INTO project_begin_date, project_begin_time, project_end_date, project_end_time
                FROM tb_project
                WHERE id = project_id;

                FOR i IN 1..3
                    LOOP
                        SELECT id
                        INTO user_id
                        FROM tb_user
                        WHERE (i = 1 AND email = 'administrator@sgdf.fr')
                           OR (i = 2 AND email = 'coordinator@sgdf.fr')
                           OR email = 'participant@sgdf.fr'
                        LIMIT 1;

                        begin_seconds := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP AT TIME ZONE
                                                             'UTC' - (project_begin_date + project_begin_time)));
                        end_seconds := EXTRACT(EPOCH FROM
                                               ((project_end_date + project_end_time) - CURRENT_TIMESTAMP AT TIME ZONE
                                                'UTC'));

                        INSERT INTO tb_project_profile (user_id, project_id, role, status, start_access_date,
                                                        start_access_time, end_access_date, end_access_time, created_by,
                                                        last_modified_by)
                        VALUES (user_id,
                                project_id,
                                CASE
                                    WHEN i = 1 THEN 'PROJECT_ADMINISTRATOR'
                                    WHEN i = 2 THEN 'PROJECT_COORDINATOR'
                                    ELSE 'PROJECT_PARTICIPANT' END,
                                CASE WHEN i = 1 THEN 'ACCEPTED' ELSE 'INVITED' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_date +
                                         (RANDOM() * (CURRENT_DATE - project_begin_date)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_time + (RANDOM() * begin_seconds) * INTERVAL '1 second' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_DATE +
                                         (RANDOM() * (project_end_date - CURRENT_DATE)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_TIME + (RANDOM() * end_seconds) * INTERVAL '1 second' END,
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1),
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1));
                    END LOOP;
            END LOOP;
    END
$$;

-- Insert data into tb_participant
DO
$$
    DECLARE
        project_id         uuid;
        project_begin_date DATE;
        project_begin_time TIME WITH TIME ZONE;
        project_end_date   DATE;
        project_end_time   TIME WITH TIME ZONE;
        begin_seconds      DOUBLE PRECISION;
        end_seconds        DOUBLE PRECISION;
    BEGIN
        FOR project_id IN SELECT id FROM tb_project
            LOOP
                SELECT begin_date, begin_time, end_date, end_time
                INTO project_begin_date, project_begin_time, project_end_date, project_end_time
                FROM tb_project
                WHERE id = project_id;

                begin_seconds := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP AT TIME ZONE
                                                     'UTC' - (project_begin_date + project_begin_time)));
                end_seconds := EXTRACT(EPOCH FROM
                                       ((project_end_date + project_end_time) - CURRENT_TIMESTAMP AT TIME ZONE 'UTC'));

                FOR i IN 1..500
                    LOOP
                        INSERT INTO tb_participant (first_name, last_name, birthday, type, start_availability_date,
                                                    start_availability_time, end_availability_date,
                                                    end_availability_time,
                                                    user_id, project_id, created_by, last_modified_by)
                        VALUES ('First ' || i,
                                'Last ' || i,
                                CASE
                                    WHEN i % 10 = 0 THEN TIMESTAMP WITH TIME ZONE '1990-01-01 00:00:00+00' + (RANDOM() *
                                                                                                              (EXTRACT(
                                                                                                                       EPOCH
                                                                                                                       FROM
                                                                                                                       ((CURRENT_TIMESTAMP - INTERVAL '18 years') -
                                                                                                                        TIMESTAMP WITH TIME ZONE '1990-01-01 00:00:00+00')) *
                                                                                                               INTERVAL '1 second'))
                                    ELSE (CURRENT_TIMESTAMP - INTERVAL '18 years') + (RANDOM() * (EXTRACT(EPOCH FROM
                                                                                                          (CURRENT_TIMESTAMP - (CURRENT_TIMESTAMP - INTERVAL '18 years'))) *
                                                                                                  INTERVAL '1 second')) END,
                                CASE WHEN i % 20 = 0 THEN 'GUEST' ELSE 'REGISTERED' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_date +
                                         (RANDOM() * (CURRENT_DATE - project_begin_date)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_time + (RANDOM() * begin_seconds) * INTERVAL '1 second' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_DATE +
                                         (RANDOM() * (project_end_date - CURRENT_DATE)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_TIME + (RANDOM() * end_seconds) * INTERVAL '1 second' END,
                                (SELECT id
                                 FROM tb_user
                                 WHERE (i = 1 AND email = 'administrator@sgdf.fr')
                                    OR (i = 2 AND email = 'coordinator@sgdf.fr')
                                    OR (i = 3 AND email = 'participant@sgdf.fr')
                                 LIMIT 1),
                                project_id,
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1),
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1));
                    END LOOP;
            END LOOP;
    END
$$;

-- Insert data into tb_group
DO
$$
    DECLARE
        project_id         uuid;
        project_begin_date DATE;
        project_begin_time TIME WITH TIME ZONE;
        project_end_date   DATE;
        project_end_time   TIME WITH TIME ZONE;
        begin_seconds      DOUBLE PRECISION;
        end_seconds        DOUBLE PRECISION;
    BEGIN
        FOR project_id IN SELECT id FROM tb_project
            LOOP
                SELECT begin_date, begin_time, end_date, end_time
                INTO project_begin_date, project_begin_time, project_end_date, project_end_time
                FROM tb_project
                WHERE id = project_id;

                begin_seconds := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP AT TIME ZONE
                                                     'UTC' - (project_begin_date + project_begin_time)));
                end_seconds := EXTRACT(EPOCH FROM
                                       ((project_end_date + project_end_time) - CURRENT_TIMESTAMP AT TIME ZONE 'UTC'));

                FOR i IN 1..20
                    LOOP
                        INSERT INTO tb_group (name, start_availability_date, start_availability_time,
                                              end_availability_date,
                                              end_availability_time, project_id, created_by, last_modified_by)
                        VALUES ('Group ' || i,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_date +
                                         (RANDOM() * (CURRENT_DATE - project_begin_date)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_time + (RANDOM() * begin_seconds) * INTERVAL '1 second' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_DATE +
                                         (RANDOM() * (project_end_date - CURRENT_DATE)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_TIME + (RANDOM() * end_seconds) * INTERVAL '1 second' END,
                                project_id,
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1),
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1));
                    END LOOP;
            END LOOP;
    END
$$;

-- Insert data into tb_group_content
DO
$$
    DECLARE
        current_project_id uuid;
        current_group_id   uuid;
        member_id          uuid;
    BEGIN
        FOR current_group_id IN SELECT id FROM tb_group
            LOOP
                SELECT project_id INTO current_project_id FROM tb_group WHERE id = current_group_id;

                FOR i IN 1..25
                    LOOP
                        SELECT id
                        INTO member_id
                        FROM tb_participant
                        WHERE project_id = current_project_id
                          AND type = 'REGISTERED'
                        ORDER BY RANDOM()
                        LIMIT 1;

                        INSERT INTO tb_group_content (group_id, participant_id)
                        SELECT current_group_id, member_id
                        WHERE NOT EXISTS (SELECT 1
                                          FROM tb_group_content
                                          WHERE group_id = current_group_id
                                            AND participant_id = member_id);
                    END LOOP;
            END LOOP;
    END
$$;

-- Insert data into tb_vehicle
DO
$$
    DECLARE
        project_id         uuid;
        project_begin_date DATE;
        project_begin_time TIME WITH TIME ZONE;
        project_end_date   DATE;
        project_end_time   TIME WITH TIME ZONE;
        begin_seconds      DOUBLE PRECISION;
        end_seconds        DOUBLE PRECISION;
    BEGIN
        FOR project_id IN SELECT id FROM tb_project
            LOOP
                SELECT begin_date, begin_time, end_date, end_time
                INTO project_begin_date, project_begin_time, project_end_date, project_end_time
                FROM tb_project
                WHERE id = project_id;

                begin_seconds := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP AT TIME ZONE
                                                     'UTC' - (project_begin_date + project_begin_time)));
                end_seconds := EXTRACT(EPOCH FROM
                                       ((project_end_date + project_end_time) - CURRENT_TIMESTAMP AT TIME ZONE 'UTC'));

                FOR i IN 1..15
                    LOOP
                        INSERT INTO tb_vehicle (license_plate, brand, model, start_availability_date,
                                                start_availability_time, end_availability_date, end_availability_time,
                                                project_id, created_by, last_modified_by)
                        VALUES ('AB-' || LPAD((i * 2)::TEXT, 3, '0') || '-DC',
                                'Brand ' || i,
                                'Model ' || i,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_date +
                                         (RANDOM() * (CURRENT_DATE - project_begin_date)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_time + (RANDOM() * begin_seconds) * INTERVAL '1 second' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_DATE +
                                         (RANDOM() * (project_end_date - CURRENT_DATE)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_TIME + (RANDOM() * end_seconds) * INTERVAL '1 second' END,
                                project_id,
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1),
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1));
                    END LOOP;
            END LOOP;
    END
$$;

-- Insert data into tb_activity
DO
$$
    DECLARE
        project_id         uuid;
        project_begin_date DATE;
        project_begin_time TIME WITH TIME ZONE;
        project_end_date   DATE;
        project_end_time   TIME WITH TIME ZONE;
        begin_seconds      DOUBLE PRECISION;
        end_seconds        DOUBLE PRECISION;
    BEGIN
        FOR project_id IN SELECT id FROM tb_project
            LOOP
                SELECT begin_date, begin_time, end_date, end_time
                INTO project_begin_date, project_begin_time, project_end_date, project_end_time
                FROM tb_project
                WHERE id = project_id;

                begin_seconds := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP AT TIME ZONE
                                                     'UTC' - (project_begin_date + project_begin_time)));
                end_seconds := EXTRACT(EPOCH FROM
                                       ((project_end_date + project_end_time) - CURRENT_TIMESTAMP AT TIME ZONE 'UTC'));

                FOR i IN 1..7
                    LOOP
                        INSERT INTO tb_activity (name, description, duration, min_allowed_participants,
                                                 max_allowed_participants, start_availability_date,
                                                 start_availability_time, end_availability_date, end_availability_time,
                                                 project_id, created_by, last_modified_by)
                        VALUES ('Activity ' || i,
                                'Description ' || i,
                                'PT' || (i % 10 + 1) || 'H',
                                FLOOR(RANDOM() * (5 - 1 + 1) + 1),
                                FLOOR(RANDOM() * (20 - 5 + 1) + 20),
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_date +
                                         (RANDOM() * (CURRENT_DATE - project_begin_date)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE project_begin_time + (RANDOM() * begin_seconds) * INTERVAL '1 second' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_DATE +
                                         (RANDOM() * (project_end_date - CURRENT_DATE)) * INTERVAL '1 day' END,
                                CASE
                                    WHEN i = 1 THEN NULL
                                    ELSE CURRENT_TIME + (RANDOM() * end_seconds) * INTERVAL '1 second' END,
                                project_id,
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1),
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1));
                    END LOOP;
            END LOOP;
    END
$$;

-- Insert data into tb_movement
DO
$$
    DECLARE
        current_project_id         uuid;
        current_project_begin_date DATE;
        current_project_begin_time TIME WITH TIME ZONE;
        current_project_end_date   DATE;
        current_project_end_time   TIME WITH TIME ZONE;
        random_interval            INTERVAL;
    BEGIN
        FOR current_project_id IN SELECT id FROM tb_project
            LOOP
                SELECT begin_date, begin_time, end_date, end_time
                INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
                FROM tb_project
                WHERE id = current_project_id;

                FOR i IN 1..4000
                    LOOP
                        random_interval :=
                                (RANDOM() *
                                 EXTRACT(EPOCH FROM ((current_project_begin_date + current_project_begin_time) -
                                                     (current_project_end_date + current_project_end_time)))) *
                                INTERVAL '1 second';

                        INSERT INTO tb_movement (date_time, type, reason, activity_id, project_id, created_by,
                                                 last_modified_by)
                        VALUES (current_project_begin_date + random_interval,
                                CASE WHEN i % 2 = 0 THEN 'IN' ELSE 'OUT' END,
                                CASE
                                    WHEN i % 5 != 0 AND i % 15 != 0 THEN (SELECT UNNEST(
                                                                                         CASE
                                                                                             WHEN i % 2 = 0
                                                                                                 THEN ARRAY ['SHOPPING', 'MEDICAL', 'OTHER'] END
                                                                                 )
                                                                          ORDER BY RANDOM()
                                                                          LIMIT 1) END,
                                CASE
                                    WHEN i % 5 = 0 AND i % 15 != 0 THEN (SELECT id
                                                                         FROM tb_activity ta
                                                                         WHERE ta.project_id = current_project_id
                                                                         ORDER BY RANDOM()
                                                                         LIMIT 1) END,
                                current_project_id,
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1),
                                (SELECT id FROM tb_user ORDER BY RANDOM() LIMIT 1));
                    END LOOP;
            END LOOP;
    END
$$;

-- Insert data into tb_movement_content
DO
$$
    DECLARE
        current_project_id    uuid;
        current_movement_id   uuid;
        participant_number    INTEGER;
        new_pool_name         VARCHAR;
        new_participant_id    uuid;
        new_participant_major BOOLEAN;
        new_vehicle_id        uuid;
    BEGIN
        FOR current_movement_id IN SELECT id FROM tb_movement
            LOOP
                SELECT project_id INTO current_project_id FROM tb_movement WHERE id = current_movement_id;
                SELECT FLOOR(RANDOM() * (30 - 1 + 1) + 1) INTO participant_number;
                SELECT name
                INTO new_pool_name
                FROM tb_group
                WHERE project_id = current_project_id
                ORDER BY RANDOM()
                LIMIT 1;

                FOR i IN 1..participant_number
                    LOOP
                        SELECT id, birthday <= (CURRENT_TIMESTAMP - INTERVAL '18 years')
                        INTO new_participant_id, new_participant_major
                        FROM tb_participant
                        WHERE project_id = current_project_id
                          AND type = 'REGISTERED'
                        ORDER BY RANDOM()
                        LIMIT 1;
                        new_vehicle_id = CASE
                                             WHEN i % 2 = 0 THEN CASE
                                                                     WHEN new_participant_major THEN (SELECT id
                                                                                                      FROM tb_vehicle tv
                                                                                                      WHERE tv.project_id = current_project_id
                                                                                                      ORDER BY RANDOM()
                                                                                                      LIMIT 1) END END;

                        INSERT INTO tb_movement_content (movement_id, participant_id, pool_name, vehicle_id)
                        SELECT current_movement_id,
                               new_participant_id,
                               CASE
                                   WHEN participant_number > 4
                                       THEN CASE WHEN i % 10 = 0 THEN NULL ELSE new_pool_name END END,
                               new_vehicle_id
                        WHERE NOT EXISTS (SELECT 1
                                          FROM tb_movement_content
                                          WHERE movement_id = current_movement_id
                                            AND participant_id = new_participant_id)
                          AND (new_vehicle_id IS NULL OR NOT EXISTS (SELECT 1
                                                                     FROM tb_movement_content
                                                                     WHERE movement_id = current_movement_id
                                                                       AND vehicle_id = new_vehicle_id));
                    END LOOP;
            END LOOP;
    END
$$;
