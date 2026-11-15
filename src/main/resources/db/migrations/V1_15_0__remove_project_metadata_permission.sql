-- Project options moved to the public metadata API: the dedicated permission is no longer needed
DELETE FROM tb_user_role_permission WHERE permission = 'REGISTRY_PROJECT_METADATA_R';
DELETE FROM tb_user_permission WHERE name = 'REGISTRY_PROJECT_METADATA_R';
