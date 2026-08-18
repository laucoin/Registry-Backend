-- Clear all data from the database
TRUNCATE TABLE tb_movement_content, tb_communication, tb_movement, tb_activity, tb_vehicle, tb_group_content, tb_group, tb_participant, tb_project_profile, tb_project, tb_preferences, tb_user CASCADE;

-- Insert data related to users
DO
$$
    DECLARE
global_admin_id           UUID = '9cd10ea7-96c1-4f82-8366-d11d2e3ec300';
        global_coordinator_id
UUID = 'fd705f30-3cbd-478d-ab37-107724321dca';
        global_user_id
UUID = 'e22a08da-b8b8-4b78-86c8-8557ddfbb945';
        global_blocked_user_id
UUID = 'e68014a3-5ac0-42cf-8b85-67d6183f6d69';
        global_blocked_profile_id
UUID = 'da39968a-c90f-46e1-b3dd-81988458ea0f';
        global_unverified_id
UUID = 'eba79b3d-2fcb-4922-805c-2c5a1eadecc7';
BEGIN
        -- Insert service account. Its email stays NULL (see
        -- V1_11_0__remove_service_account_email.sql): the unique index on tb_user.email
        -- is global, but findByEmail excludes SERVICE_ACCOUNT rows, so any address held
        -- here is invisible to the first-login lookup yet still blocks the INSERT.
INSERT INTO tb_user(type, first_name, last_name)
VALUES ('SERVICE_ACCOUNT', 'Luc', 'AUCOIN');

-- Insert users
-- oidc_id is NULL on purpose: these are "invited" (email-only) accounts. On first
-- login via Authentik, the backend links the IdP UUID to the matching email (see
-- TokenConverterService), so you sign in as one of these seeded users. The
-- Authentik users are provisioned by local-dev/authentik/blueprints/registry.yaml.
-- One exception: the IdP does not vouch for `unverified@sgdf.fr` (its Authentik
-- account carries email_verified: false), so that link is refused with
-- AUTH_EMAIL_NOT_VERIFIED and the row stays unclaimed run after run. It exists to
-- make that refusal reproducible by hand — sign in as `unverified`.
INSERT INTO tb_user (id, oidc_id, type, first_name, last_name, email, role, birthday, last_login, created_by,
                     last_modified_by, visible)
VALUES (global_admin_id, NULL, 'USER', 'Jane', 'SMITH',
        'administrator@sgdf.fr', 'USER_ADMINISTRATOR', '1980-01-01', '2025-03-01 15:28:51.144372 +00:00',
        global_admin_id, global_admin_id, TRUE),
       (global_coordinator_id, NULL, 'USER', 'John', 'DOE',
        'coordinator@sgdf.fr', 'USER', '1990-01-01', '2025-03-01 15:28:51.144372 +00:00', global_coordinator_id,
        global_coordinator_id, TRUE),
       (global_user_id, NULL, 'USER', 'Charles', 'PINA',
        'participant@sgdf.fr', 'USER', '2000-01-01', '2025-03-01 15:28:51.144372 +00:00', global_user_id,
        global_user_id, TRUE),
       (global_blocked_user_id, NULL, 'USER', 'Aliyah', 'NIELSEN',
        'blocked-user@sgdf.fr', 'USER_ADMINISTRATOR', '1970-01-01', '2025-03-01 15:28:51.144372 +00:00',
        global_blocked_user_id, global_blocked_user_id, FALSE),
       (global_blocked_profile_id, NULL, 'USER', 'Emil', 'BRADFORD',
        'blocked-profile@sgdf.fr', 'USER_ADMINISTRATOR', '1960-01-01', '2025-03-01 15:28:51.144372 +00:00',
        global_blocked_profile_id, global_blocked_profile_id, TRUE),
       (global_unverified_id, NULL, 'USER', 'Nina', 'VOGEL',
        'unverified@sgdf.fr', 'USER', '1995-01-01', '2025-03-01 15:28:51.144372 +00:00',
        global_unverified_id, global_unverified_id, TRUE);

-- Insert preferences for users
INSERT INTO tb_preferences (user_id, created_by, last_modified_by)
VALUES (global_admin_id, global_admin_id, global_admin_id),
       (global_coordinator_id, global_coordinator_id, global_coordinator_id),
       (global_user_id, global_user_id, global_user_id),
       (global_blocked_user_id, global_blocked_user_id, global_blocked_user_id),
       (global_blocked_profile_id, global_blocked_profile_id, global_blocked_profile_id),
       (global_unverified_id, global_unverified_id, global_unverified_id);
END
$$;

-- Insert projects and make them usable
DO
$$
    DECLARE
global_admin_id            UUID = '9cd10ea7-96c1-4f82-8366-d11d2e3ec300';
        global_coordinator_id
UUID = 'fd705f30-3cbd-478d-ab37-107724321dca';
        global_user_id
UUID = 'e22a08da-b8b8-4b78-86c8-8557ddfbb945';
        global_blocked_user_id
UUID = 'e68014a3-5ac0-42cf-8b85-67d6183f6d69';
        global_blocked_profile_id
UUID = 'da39968a-c90f-46e1-b3dd-81988458ea0f';
        global_project_id
UUID = 'b7432b97-cfc6-4109-aaaa-38d348523f1e';
        global_project_profile_id
UUID = '28d92461-addb-42d5-9301-18ef6e966608';
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
BEGIN
        -- Insert projects
INSERT INTO tb_project (id, name, begin_date, begin_time, end_date, end_time, created_by, last_modified_by,
                        visible)
SELECT CASE WHEN i = 1 THEN global_project_id ELSE gen_random_uuid() END,
       'Project ' || i,
       NOW() - (RANDOM() * INTERVAL '30 days'),
       NOW() - (RANDOM() * INTERVAL '30 minutes'),
       NOW() + (RANDOM() * INTERVAL '30 days'),
       NOW() + (RANDOM() * INTERVAL '30 minutes'),
       global_admin_id,
       global_admin_id,
       i != 15
FROM GENERATE_SERIES(1, 15) AS i;

FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

INSERT INTO tb_project_profile (id, user_id, project_id, role, status, start_access_date,
                                start_access_time, end_access_date, end_access_time, created_by,
                                last_modified_by, visible)
VALUES (CASE
            WHEN current_project_id = global_project_id THEN global_project_profile_id
            ELSE gen_random_uuid() END, global_admin_id, current_project_id, 'PROJECT_ADMINISTRATOR',
        'ACCEPTED', NULL, NULL, NULL, NULL, global_admin_id, global_admin_id, TRUE),
       (gen_random_uuid(), global_coordinator_id, current_project_id, 'PROJECT_COORDINATOR', 'INVITED',
        current_project_begin_date +
        (RANDOM() * (CURRENT_DATE - current_project_begin_date)) * INTERVAL '1 day',
        current_project_begin_time + (RANDOM() * EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP AT TIME ZONE
            'UTC' -
            (current_project_begin_date + current_project_begin_time)))) *
            INTERVAL '1 second',
        CURRENT_DATE + (RANDOM() * (current_project_end_date - CURRENT_DATE)) * INTERVAL '1 day',
        CURRENT_TIME + (RANDOM() * EXTRACT(EPOCH FROM
                                           ((current_project_end_date + current_project_end_time) -
                                            CURRENT_TIMESTAMP AT TIME ZONE 'UTC'))) *
            INTERVAL '1 second', global_admin_id, global_admin_id, TRUE),
       (gen_random_uuid(), global_user_id, current_project_id, 'PROJECT_PARTICIPANT', 'INVITED',
        current_project_begin_date +
        (RANDOM() * (CURRENT_DATE - current_project_begin_date)) * INTERVAL '1 day',
        current_project_begin_time + (RANDOM() * EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP AT TIME ZONE
            'UTC' -
            (current_project_begin_date + current_project_begin_time)))) *
            INTERVAL '1 second',
        CURRENT_DATE + (RANDOM() * (current_project_end_date - CURRENT_DATE)) * INTERVAL '1 day',
        CURRENT_TIME + (RANDOM() * EXTRACT(EPOCH FROM
                                           ((current_project_end_date + current_project_end_time) -
                                            CURRENT_TIMESTAMP AT TIME ZONE 'UTC'))) *
            INTERVAL '1 second', global_admin_id, global_admin_id, TRUE),
       (gen_random_uuid(), global_blocked_user_id, current_project_id, 'PROJECT_ADMINISTRATOR',
        'ACCEPTED', NULL, NULL, NULL, NULL, global_admin_id, global_admin_id, TRUE),
       (gen_random_uuid(), global_blocked_profile_id, current_project_id, 'PROJECT_ADMINISTRATOR',
        'ACCEPTED', NULL, NULL, NULL, NULL, global_admin_id, global_admin_id, FALSE);
END LOOP;
END
$$;

-- Insert participants
DO
$$
    DECLARE
global_admin_id            UUID = '9cd10ea7-96c1-4f82-8366-d11d2e3ec300';
        global_coordinator_id
UUID = 'fd705f30-3cbd-478d-ab37-107724321dca';
        global_user_id
UUID = 'e22a08da-b8b8-4b78-86c8-8557ddfbb945';
        global_project_id
UUID = 'b7432b97-cfc6-4109-aaaa-38d348523f1e';
        global_participant_id
UUID = '88f7194e-6633-4f84-b3e3-8546b51d07e0';
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
BEGIN
FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

INSERT INTO tb_participant (id, first_name, last_name, birthday, type, start_availability_date,
                            start_availability_time, end_availability_date, end_availability_time,
                            user_id, project_id, created_by, last_modified_by, visible)
SELECT CASE
           WHEN i = 1 AND current_project_id = global_project_id THEN global_participant_id
           ELSE gen_random_uuid() END,
       'First ' || i,
       'Last ' || i,
       CASE
           WHEN i % 10 = 0 THEN TIMESTAMP
WITH TIME ZONE '1990-01-01 00:00:00+00' + (RANDOM() *
    EXTRACT (EPOCH FROM
    ((CURRENT_TIMESTAMP - INTERVAL '18 years') -
    TIMESTAMP WITH TIME ZONE '1990-01-01 00:00:00+00')) *
    INTERVAL '1 second')
    ELSE (CURRENT_TIMESTAMP - INTERVAL '18 years') + (RANDOM() * EXTRACT (EPOCH FROM
    (CURRENT_TIMESTAMP - (CURRENT_TIMESTAMP - INTERVAL '18 years'))) *
    INTERVAL '1 second')
END
,
                       CASE WHEN i % 20 = 0 THEN 'GUEST' ELSE 'REGISTERED'
END
,
                       CASE
                           WHEN i % 10 = 0 THEN current_project_begin_date +
                                                (RANDOM() * (CURRENT_DATE - current_project_begin_date)) *
                                                INTERVAL '1 day'
END
,
                       CASE
                           WHEN i % 20 = 0 THEN current_project_begin_time + (RANDOM() * EXTRACT(EPOCH FROM
                                                                                                 (CURRENT_TIMESTAMP AT TIME ZONE
                                                                                                  'UTC' -
                                                                                                  (current_project_begin_date + current_project_begin_time)))) *
                                                                             INTERVAL '1 second'
END
,
                       CASE
                           WHEN i % 10 = 0 THEN CURRENT_DATE + (RANDOM() * (current_project_end_date - CURRENT_DATE)) *
                                                               INTERVAL '1 day'
END
,
                       CASE
                           WHEN i % 20 = 0 THEN CURRENT_TIME + (RANDOM() * EXTRACT(EPOCH FROM
                                                                                   ((current_project_end_date + current_project_end_time) -
                                                                                    CURRENT_TIMESTAMP AT TIME ZONE
                                                                                    'UTC'))) * INTERVAL '1 second'
END
,
                       CASE
                           WHEN i = 1 THEN global_admin_id
                           WHEN i = 2 THEN global_coordinator_id
                           WHEN i = 3 THEN global_user_id
END
,
                       current_project_id,
                       (SELECT tpp.user_id
                        FROM tb_project_profile tpp
                        WHERE tpp.project_id = current_project_id
                          AND tpp.status = 'ACCEPTED'
                        ORDER BY RANDOM()
                        LIMIT 1),
                       (SELECT tpp.user_id
                        FROM tb_project_profile tpp
                        WHERE tpp.project_id = current_project_id
                          AND tpp.status = 'ACCEPTED'
                        ORDER BY RANDOM()
                        LIMIT 1),
                       i % 50 != 0
                FROM GENERATE_SERIES(1, 500) AS i;
END LOOP;
END
$$;

-- Insert groups
DO
$$
    DECLARE
global_project_id          UUID = 'b7432b97-cfc6-4109-aaaa-38d348523f1e';
        global_group_id
UUID = 'acb4943c-a911-4f1d-b899-69f6cfcfef90';
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
        current_group_id
UUID;
BEGIN
FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

FOR i IN 1..20
                    LOOP
                        current_group_id = CASE
                                               WHEN i = 1 AND current_project_id = global_project_id
                                                   THEN global_group_id
                                               ELSE gen_random_uuid()
END;

INSERT INTO tb_group (id, name, start_availability_date, start_availability_time,
                      end_availability_date, end_availability_time, project_id, created_by,
                      last_modified_by, visible)
VALUES (current_group_id,
        'Group ' || i,
        CASE
            WHEN i % 10 = 0 THEN current_project_begin_date +
                                 (RANDOM() * (CURRENT_DATE - current_project_begin_date)) *
            INTERVAL '1 day' END,
        CASE
            WHEN i % 20 = 0 THEN current_project_begin_time + (RANDOM() * EXTRACT(EPOCH FROM
                                                                                  (CURRENT_TIMESTAMP AT TIME ZONE
                                                                                      'UTC' -
                                                                                      (current_project_begin_date + current_project_begin_time)))) *
            INTERVAL '1 second' END,
        CASE
            WHEN i % 10 = 0 THEN CURRENT_DATE +
                                 (RANDOM() * (current_project_end_date - CURRENT_DATE)) *
            INTERVAL '1 day' END,
        CASE
            WHEN i % 20 = 0 THEN CURRENT_TIME + (RANDOM() * EXTRACT(EPOCH FROM
                                                                    ((current_project_end_date + current_project_end_time) -
                                                                     CURRENT_TIMESTAMP AT TIME ZONE
                                                                         'UTC'))) *
            INTERVAL '1 second' END,
        current_project_id,
        (SELECT tpp.user_id
         FROM tb_project_profile tpp
         WHERE tpp.project_id = current_project_id
           AND tpp.status = 'ACCEPTED'
         ORDER BY RANDOM()
            LIMIT 1),
       (SELECT tpp.user_id
           FROM tb_project_profile tpp
           WHERE tpp.project_id = current_project_id
           AND tpp.status = 'ACCEPTED'
           ORDER BY RANDOM()
           LIMIT 1), i % 10 != 0);

INSERT INTO tb_group_content (group_id, participant_id)
SELECT current_group_id,
       tp.id
FROM tb_participant tp
WHERE tp.project_id = current_project_id
  AND NOT EXISTS (SELECT 1
                  FROM tb_group_content tgc
                  WHERE tgc.group_id = current_group_id
                    AND tgc.participant_id = tp.id)
ORDER BY RANDOM() LIMIT 25;
END LOOP;
END LOOP;
END
$$;

-- Insert vehicles
DO
$$
    DECLARE
global_project_id          UUID = 'b7432b97-cfc6-4109-aaaa-38d348523f1e';
        global_vehicle_id
UUID = '7ae25102-8337-4836-93e5-dd2cd8c5d5ec';
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
BEGIN
FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

INSERT INTO tb_vehicle (id, license_plate, brand, model, start_availability_date,
                        start_availability_time, end_availability_date, end_availability_time,
                        project_id, created_by, last_modified_by, visible)
SELECT CASE
           WHEN i = 1 AND current_project_id = global_project_id THEN global_vehicle_id
           ELSE gen_random_uuid() END,
       'AB-' || LPAD((i * 2)::TEXT, 3, '0') || '-DC',
       'Brand ' || i,
       'Model ' || i,
       CASE
           WHEN i % 10 = 0 THEN current_project_begin_date +
                                (RANDOM() * (CURRENT_DATE - current_project_begin_date)) *
    INTERVAL '1 day'
END
,
                       CASE
                           WHEN i % 20 = 0 THEN current_project_begin_time + (RANDOM() * EXTRACT(EPOCH FROM
                                                                                                 (CURRENT_TIMESTAMP AT TIME ZONE
                                                                                                  'UTC' -
                                                                                                  (current_project_begin_date + current_project_begin_time)))) *
                                                                             INTERVAL '1 second'
END
,
                       CASE
                           WHEN i % 10 = 0 THEN CURRENT_DATE + (RANDOM() * (current_project_end_date - CURRENT_DATE)) *
                                                               INTERVAL '1 day'
END
,
                       CASE
                           WHEN i % 20 = 0 THEN CURRENT_TIME + (RANDOM() * EXTRACT(EPOCH FROM
                                                                                   ((current_project_end_date + current_project_end_time) -
                                                                                    CURRENT_TIMESTAMP AT TIME ZONE
                                                                                    'UTC'))) * INTERVAL '1 second'
END
,
                       current_project_id,
                       (SELECT tpp.user_id
                        FROM tb_project_profile tpp
                        WHERE tpp.project_id = current_project_id
                          AND tpp.status = 'ACCEPTED'
                        ORDER BY RANDOM()
                        LIMIT 1),
                       (SELECT tpp.user_id
                        FROM tb_project_profile tpp
                        WHERE tpp.project_id = current_project_id
                          AND tpp.status = 'ACCEPTED'
                        ORDER BY RANDOM()
                        LIMIT 1),
                       i != 15
                FROM GENERATE_SERIES(1, 15) AS i;
END LOOP;
END
$$;

-- Insert activities
DO
$$
    DECLARE
global_project_id          UUID = 'b7432b97-cfc6-4109-aaaa-38d348523f1e';
        global_activity_id
UUID = '95806471-9c01-477a-84ea-8c37fd0cc8c5';
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
BEGIN
FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

INSERT INTO tb_activity (id, name, description, duration, min_allowed_participants,
                         max_allowed_participants, start_availability_date, start_availability_time,
                         end_availability_date, end_availability_time, project_id, created_by,
                         last_modified_by, visible)
SELECT CASE
           WHEN i = 1 AND current_project_id = global_project_id THEN global_activity_id
           ELSE gen_random_uuid() END,
       'Activity ' || i,
       'Description ' || i,
       'PT' || (i % 10 + 1) || 'H',
       FLOOR(RANDOM() * (5 - 1 + 1) + 1),
       FLOOR(RANDOM() * (20 - 5 + 1) + 20),
       CASE
           WHEN i % 10 = 0 THEN current_project_begin_date +
                                (RANDOM() * (CURRENT_DATE - current_project_begin_date)) *
    INTERVAL '1 day'
END
,
                       CASE
                           WHEN i % 20 = 0 THEN current_project_begin_time + (RANDOM() * EXTRACT(EPOCH FROM
                                                                                                 (CURRENT_TIMESTAMP AT TIME ZONE
                                                                                                  'UTC' -
                                                                                                  (current_project_begin_date + current_project_begin_time)))) *
                                                                             INTERVAL '1 second'
END
,
                       CASE
                           WHEN i % 10 = 0 THEN CURRENT_DATE + (RANDOM() * (current_project_end_date - CURRENT_DATE)) *
                                                               INTERVAL '1 day'
END
,
                       CASE
                           WHEN i % 20 = 0 THEN CURRENT_TIME + (RANDOM() * EXTRACT(EPOCH FROM
                                                                                   ((current_project_end_date + current_project_end_time) -
                                                                                    CURRENT_TIMESTAMP AT TIME ZONE
                                                                                    'UTC'))) * INTERVAL '1 second'
END
,
                       current_project_id,
                       (SELECT tpp.user_id
                        FROM tb_project_profile tpp
                        WHERE tpp.project_id = current_project_id
                          AND tpp.status = 'ACCEPTED'
                        ORDER BY RANDOM()
                        LIMIT 1),
                       (SELECT tpp.user_id
                        FROM tb_project_profile tpp
                        WHERE tpp.project_id = current_project_id
                          AND tpp.status = 'ACCEPTED'
                        ORDER BY RANDOM()
                        LIMIT 1),
                       i != 15
                FROM GENERATE_SERIES(1, 15) AS i;
END LOOP;
END
$$;

-- Insert movements for registered participants
DO
$$
    DECLARE
chunk_size                          INT  = 100;
        global_project_id
UUID = 'b7432b97-cfc6-4109-aaaa-38d348523f1e';
        global_movement_id
UUID = '63f4c4e8-bd07-445b-8a6e-899ac490cf0c';
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
        current_movement_id
UUID;
        current_movement_type
VARCHAR;
        current_movement_has_activity
BOOLEAN;
        current_movement_participant_number
INTEGER;
        current_movement_participant_id
UUID;
        current_movement_participant_major
BOOLEAN;
        current_movement_pool_name
VARCHAR;
        current_movement_vehicle_id
UUID;
BEGIN
FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

FOR i IN 1..4000
                    LOOP
                        current_movement_id = CASE
                                                  WHEN i = 1 AND current_project_id = global_project_id
                                                      THEN global_movement_id
                                                  ELSE gen_random_uuid()
END;
                        current_movement_type
= CASE WHEN i % 2 = 0 THEN 'IN' ELSE 'OUT'
END;
                        current_movement_has_activity
= current_movement_type = 'OUT' AND i % 5 = 0;

INSERT INTO tb_movement (id, date_time, type, reason, activity_id, project_id, created_by,
                         last_modified_by, visible)
VALUES (current_movement_id,
        current_project_begin_date + (RANDOM() * EXTRACT(EPOCH FROM
                                                         ((current_project_begin_date + current_project_begin_time) -
                                                          (current_project_end_date + current_project_end_time)))) *
            INTERVAL '1 second',
        current_movement_type,
        CASE
            WHEN current_movement_type = 'OUT' AND NOT current_movement_has_activity
                THEN (SELECT UNNEST(ARRAY['SHOPPING', 'MEDICAL', 'OTHER'])
                      ORDER BY RANDOM()
                LIMIT 1)
END
,
                                CASE
                                    WHEN current_movement_has_activity THEN (SELECT id
                                                                             FROM tb_activity ta
                                                                             WHERE ta.project_id = current_project_id
                                                                             ORDER BY RANDOM()
                                                                             LIMIT 1)
END
,
                                current_project_id,
                                (SELECT tpp.user_id
                                 FROM tb_project_profile tpp
                                 WHERE tpp.project_id = current_project_id
                                   AND tpp.status = 'ACCEPTED'
                                 ORDER BY RANDOM()
                                 LIMIT 1),
                                (SELECT tpp.user_id
                                 FROM tb_project_profile tpp
                                 WHERE tpp.project_id = current_project_id
                                   AND tpp.status = 'ACCEPTED'
                                 ORDER BY RANDOM()
                                 LIMIT 1),
                                i % 50 != 0);

SELECT FLOOR(RANDOM() * 30 + 1)
INTO current_movement_participant_number;
SELECT name
INTO current_movement_pool_name
FROM tb_group
WHERE project_id = current_project_id
ORDER BY RANDOM() LIMIT 1;

FOR i IN 1..current_movement_participant_number
                            LOOP
SELECT id, birthday <= (CURRENT_TIMESTAMP - INTERVAL '18 years')
INTO current_movement_participant_id, current_movement_participant_major
FROM tb_participant
WHERE project_id = current_project_id
  AND type = 'REGISTERED'
ORDER BY RANDOM() LIMIT 1;
current_movement_vehicle_id
= CASE
                                                                  WHEN i % 2 = 0 THEN CASE
                                                                                          WHEN current_movement_participant_major
                                                                                              THEN (SELECT id
                                                                                                    FROM tb_vehicle tv
                                                                                                    WHERE tv.project_id = current_project_id
                                                                                                    ORDER BY RANDOM()
                                                                                                    LIMIT 1)
END
END;

INSERT INTO tb_movement_content (movement_id, participant_id, pool_name, vehicle_id)
SELECT current_movement_id,
       current_movement_participant_id,
       CASE
           WHEN current_movement_participant_number > 4
               THEN CASE WHEN i % 10 = 0 THEN NULL ELSE current_movement_pool_name END END,
       current_movement_vehicle_id WHERE NOT EXISTS (SELECT 1
                                                  FROM tb_movement_content
                                                  WHERE movement_id = current_movement_id
                                                    AND participant_id = current_movement_participant_id)
                                  AND (current_movement_vehicle_id IS NULL OR NOT EXISTS (SELECT 1
                                                                                          FROM tb_movement_content
                                                                                          WHERE movement_id = current_movement_id
                                                                                            AND vehicle_id = current_movement_vehicle_id));
END LOOP;

                        IF
i % chunk_size = 0 THEN
                            COMMIT;
END IF;
END LOOP;
END LOOP;
COMMIT;
END
$$;

-- Insert movements for guest participants
DO
$$
    DECLARE
chunk_size                 INT = 100;
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
        current_movement_id
UUID;
        current_movement_type
VARCHAR;
BEGIN
FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

FOR i IN 1..100
                    LOOP
                        current_movement_id = gen_random_uuid();
                        current_movement_type
= CASE WHEN i % 2 = 0 THEN 'IN' ELSE 'OUT'
END;

INSERT INTO tb_movement (id, date_time, type, reason, project_id, created_by, last_modified_by,
                         visible)
VALUES (current_movement_id,
        current_project_begin_date + (RANDOM() * EXTRACT(EPOCH FROM
                                                         ((current_project_begin_date + current_project_begin_time) -
                                                          (current_project_end_date + current_project_end_time)))) *
            INTERVAL '1 second',
        current_movement_type,
        CASE
            WHEN current_movement_type = 'IN'
                THEN (SELECT UNNEST(ARRAY['EMERGENCY', 'LOGISTICS', 'PARTNER_ANIMATION', 'VISIT'])
                      ORDER BY RANDOM()
                LIMIT 1)
END
,
                                current_project_id,
                                (SELECT tpp.user_id
                                 FROM tb_project_profile tpp
                                 WHERE tpp.project_id = current_project_id
                                   AND tpp.status = 'ACCEPTED'
                                 ORDER BY RANDOM()
                                 LIMIT 1),
                                (SELECT tpp.user_id
                                 FROM tb_project_profile tpp
                                 WHERE tpp.project_id = current_project_id
                                   AND tpp.status = 'ACCEPTED'
                                 ORDER BY RANDOM()
                                 LIMIT 1),
                                i % 50 != 0);

INSERT INTO tb_movement_content (movement_id, participant_id)
SELECT current_movement_id,
       tp.id
FROM tb_participant tp
WHERE tp.project_id = current_project_id
  AND tp.type = 'GUEST'
  AND NOT EXISTS (SELECT 1
                  FROM tb_movement_content tmc
                  WHERE tmc.movement_id = current_movement_id
                    AND tmc.participant_id = tp.id)
ORDER BY RANDOM() LIMIT FLOOR(RANDOM() * 10 + 1);

IF
i % chunk_size = 0 THEN
                            COMMIT;
END IF;
END LOOP;
END LOOP;
COMMIT;
END
$$;

-- Insert movements' communications
DO
$$
    DECLARE
current_movement_id        UUID;
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
BEGIN
FOR current_movement_id IN
SELECT id
FROM tb_movement
WHERE activity_id IS NOT NULL
    LOOP
SELECT tp.id, tp.begin_date, tp.begin_time, tp.end_date, tp.end_time
INTO current_project_id, current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_movement tm
         INNER JOIN tb_project tp ON tm.project_id = tp.id
WHERE tm.id = current_movement_id;

INSERT INTO tb_communication (date_time, message, movement_id, project_id, created_by, last_modified_by,
                              visible)
SELECT current_project_begin_date + (RANDOM() * EXTRACT(EPOCH FROM
                                                        ((current_project_begin_date + current_project_begin_time) -
                                                         (current_project_end_date + current_project_end_time)))) *
    INTERVAL '1 second'
     , 'Message ' || i
     , current_movement_id
     , current_project_id
     , (SELECT tpp.user_id
    FROM tb_project_profile tpp
    WHERE tpp.project_id = current_project_id
    AND tpp.status = 'ACCEPTED'
    ORDER BY RANDOM()
    LIMIT 1)
     , (SELECT tpp.user_id
    FROM tb_project_profile tpp
    WHERE tpp.project_id = current_project_id
    AND tpp.status = 'ACCEPTED'
    ORDER BY RANDOM()
    LIMIT 1)
     , i != 50
FROM GENERATE_SERIES(1, 50) AS i;
END LOOP;
END
$$;

-- Make sure we know a communication ID
DO
$$
    DECLARE
global_communication_id UUID = '64303545-0826-4efe-9f60-43b1219f75dc';
BEGIN
        IF
NOT EXISTS (SELECT 1 FROM tb_communication WHERE id = global_communication_id) THEN
UPDATE tb_communication
SET id = global_communication_id
WHERE id = (SELECT id FROM tb_communication ORDER BY id LIMIT 1);
END IF;
END
$$;

-- Insert alerts
DO
$$
    DECLARE
global_project_id          UUID = 'b7432b97-cfc6-4109-aaaa-38d348523f1e';
        global_alert_id
UUID = 'da5ae275-d828-4738-ac47-367fdad1bff4';
        current_project_id
UUID;
        current_project_begin_date
DATE;
        current_project_begin_time
TIME WITH TIME ZONE;
        current_project_end_date
DATE;
        current_project_end_time
TIME WITH TIME ZONE;
        current_alert_id
UUID;
BEGIN
FOR current_project_id IN
SELECT id
FROM tb_project LOOP
SELECT begin_date, begin_time, end_date, end_time
INTO current_project_begin_date, current_project_begin_time, current_project_end_date, current_project_end_time
FROM tb_project
WHERE id = current_project_id;

FOR i IN 1..100
                    LOOP
                        current_alert_id = CASE
                                               WHEN i = 1 AND current_project_id = global_project_id
                                                   THEN global_alert_id
                                               ELSE gen_random_uuid()
END;

INSERT INTO tb_alert (id, title, status, project_id, created_by, last_modified_by, visible)
VALUES (current_alert_id,
        'Alert ' || i,
        CASE
            WHEN i % 10 = 0 THEN 'IN_PROGRESS'
            WHEN i % 20 = 0 THEN 'CANCELED'
            ELSE 'RESOLVED' END,
        current_project_id,
        (SELECT tpp.user_id
         FROM tb_project_profile tpp
         WHERE tpp.project_id = current_project_id
           AND tpp.status = 'ACCEPTED'
         ORDER BY RANDOM()
            LIMIT 1),
       (SELECT tpp.user_id
           FROM tb_project_profile tpp
           WHERE tpp.project_id = current_project_id
           AND tpp.status = 'ACCEPTED'
           ORDER BY RANDOM()
           LIMIT 1), i % 50 != 0);

UPDATE tb_communication
SET alert_id = current_alert_id
WHERE id IN (SELECT id
             FROM tb_communication
             WHERE project_id = current_project_id
               AND alert_id IS NULL
             ORDER BY RANDOM()
    LIMIT 2);

INSERT INTO tb_communication (date_time, message, alert_id, project_id, created_by,
                              last_modified_by, visible)
SELECT current_project_begin_date + (RANDOM() * EXTRACT(EPOCH FROM
                                                        ((current_project_begin_date + current_project_begin_time) -
                                                         (current_project_end_date + current_project_end_time)))) *
    INTERVAL '1 second'
     , 'Message ' || j
     , current_alert_id
     , current_project_id
     , (SELECT tpp.user_id
    FROM tb_project_profile tpp
    WHERE tpp.project_id = current_project_id
    AND tpp.status = 'ACCEPTED'
    ORDER BY RANDOM()
    LIMIT 1)
     , (SELECT tpp.user_id
    FROM tb_project_profile tpp
    WHERE tpp.project_id = current_project_id
    AND tpp.status = 'ACCEPTED'
    ORDER BY RANDOM()
    LIMIT 1)
     , i != 50
FROM GENERATE_SERIES(1, 10) AS j;
END LOOP;
END LOOP;
END
$$;
