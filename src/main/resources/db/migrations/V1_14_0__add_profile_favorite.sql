-- Favorites live on the membership row (per-user, per-project). A user
-- stars their own profile; the project is pinned on their home dashboard.
ALTER TABLE tb_project_profile
    ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE;

-- The home lists the caller's starred projects: a partial index keeps that
-- per-user filtered read cheap.
CREATE INDEX tb_project_profile_index_favorite
    ON tb_project_profile (user_id) WHERE favorite IS TRUE;
