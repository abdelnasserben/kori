CREATE TABLE IF NOT EXISTS platform_config (
    id integer PRIMARY KEY,
    agent_cash_limit_global numeric(19,2) NOT NULL
);

INSERT INTO platform_config (id, agent_cash_limit_global)
VALUES (1, 0.00) -- TODO(slice-2): tune conservative default cash limit when policy is enforced
ON CONFLICT (id) DO NOTHING;