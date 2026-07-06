ALTER TABLE breeze_module_registry
    ADD COLUMN IF NOT EXISTS updated_by CHAR(36) NULL;