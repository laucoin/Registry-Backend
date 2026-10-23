-- tb_project_favorite definition
CREATE TABLE tb_project_favorite
(
    id           uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    user_id      uuid                     NOT NULL,
    project_id   uuid                     NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tb_project_favorite_pkey PRIMARY KEY (id),
    CONSTRAINT tb_project_favorite_unique_user_project UNIQUE (user_id, project_id)
);

-- tb_project_favorite foreign keys and indexes
CREATE INDEX tb_project_favorite_index_project_id ON tb_project_favorite (project_id);

ALTER TABLE tb_project_favorite
    ADD CONSTRAINT tb_project_favorite_user_fkey FOREIGN KEY (user_id) REFERENCES tb_user (id) ON DELETE CASCADE;
ALTER TABLE tb_project_favorite
    ADD CONSTRAINT tb_project_favorite_project_fkey FOREIGN KEY (project_id) REFERENCES tb_project (id) ON DELETE CASCADE;
