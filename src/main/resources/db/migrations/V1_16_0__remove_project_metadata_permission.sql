-- REGISTRY_PROJECT_METADATA_R is no longer checked: GET /projects/options now only requires authentication,
-- same access rule as the other /metadata endpoints. Cascades to tb_user_role_permission.
DELETE FROM tb_user_permission
WHERE name = 'REGISTRY_PROJECT_METADATA_R';
