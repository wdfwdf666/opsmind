ALTER TABLE agent_run
    ADD COLUMN task_type_source VARCHAR(32) NOT NULL DEFAULT 'EXPLICIT';

ALTER TABLE agent_run
    ADD COLUMN planner_provider VARCHAR(32) NOT NULL DEFAULT 'legacy';

ALTER TABLE agent_run
    ADD COLUMN planner_model VARCHAR(200) NOT NULL DEFAULT 'legacy-deterministic';

ALTER TABLE agent_run
    ADD COLUMN planner_reason VARCHAR(500) NOT NULL DEFAULT 'Run created before FEAT-018';
