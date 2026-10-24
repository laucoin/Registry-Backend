-- Re-enabling (and listing disabled elements) is reserved to the project administrator, which holds the delete permissions:
-- the coordinator loses the movement deletion so that it can no longer re-enable a movement
DELETE FROM tb_project_role_permission WHERE role = 'PROJECT_COORDINATOR' AND permission = 'REGISTRY_PROJECT_MOVEMENT_D';
