CREATE TABLE monitors (
          id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
          user_id          BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
          name             VARCHAR(100)  NOT NULL,
          target           VARCHAR(2048) NOT NULL,
          type             VARCHAR(10)   NOT NULL,
          interval_seconds INT           NOT NULL,
          timeout_seconds  INT           NOT NULL,
          expected_status  INT,
          enabled          BOOLEAN       NOT NULL DEFAULT TRUE,
          created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
          CONSTRAINT chk_monitors_type CHECK (type IN ('HTTP', 'TCP')),
          CONSTRAINT chk_monitors_interval CHECK (interval_seconds >= 30),
          CONSTRAINT chk_monitors_timeout CHECK (timeout_seconds > 0 AND timeout_seconds < interval_seconds)
);

CREATE INDEX idx_monitors_user_id ON monitors (user_id);