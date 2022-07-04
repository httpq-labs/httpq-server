SET enable_partition_pruning = on;

-- tenants
CREATE TABLE tenants (
	id      	UUID NOT NULL UNIQUE PRIMARY KEY,
	name     	VARCHAR(128) NOT NULL,
	created_at 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE security_keys (
	id      	   UUID NOT NULL UNIQUE PRIMARY KEY,
	security_key  UUID NOT NULL UNIQUE,
	scope     VARCHAR(16),
	parent_key   UUID REFERENCES security_keys,
	tenant_id	UUID REFERENCES tenants ON DELETE CASCADE,
	created_at 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- only one root key
CREATE UNIQUE INDEX on security_keys (scope) where scope = 'ROOT';
-- only one root key without a parent
CREATE UNIQUE INDEX ON security_keys ((parent_key IS NULL)) WHERE parent_key IS NULL;
CREATE UNIQUE INDEX ON security_keys (tenant_id, scope) WHERE scope = 'TENANT_MASTER';
-- bootstrap root key
INSERT INTO security_keys (id, security_key, scope, parent_key)
VALUES (gen_random_uuid(), gen_random_uuid(), 'ROOT', null);

-- versions
CREATE TABLE versions (
	id      	UUID NOT NULL PRIMARY KEY,
	tenant_id	UUID REFERENCES tenants ON DELETE CASCADE,
	version     VARCHAR(16) NOT NULL,
	created_at 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UNIQUE(tenant_id, version)
);


-- topics
CREATE TABLE topics (
	id      	UUID NOT NULL UNIQUE PRIMARY KEY,
	tenant_id	UUID REFERENCES tenants ON DELETE CASCADE,
	name     	VARCHAR(128) NOT NULL,
	created_at 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UNIQUE(tenant_id, name)
);

-- consumers
CREATE TABLE consumers (
  id      	UUID NOT NULL UNIQUE PRIMARY KEY,
	tenant_id	UUID REFERENCES tenants ON DELETE CASCADE,
	external_id VARCHAR(256),
	version_id	UUID REFERENCES versions NOT NULL,
	created_at 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	UNIQUE(tenant_id, external_id)
);

-- subscriptions
CREATE TABLE subscriptions (
  id      	UUID NOT NULL UNIQUE PRIMARY KEY,
	tenant_id		UUID REFERENCES tenants ON DELETE CASCADE,
	consumer_id		UUID REFERENCES consumers ON DELETE CASCADE,
	topic_id		UUID REFERENCES topics ON DELETE CASCADE,
	url				VARCHAR(1024) NOT NULL,
	created_at 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at 		TIMESTAMP WITH TIME ZONE,
	is_active		BOOLEAN NOT NULL,
	is_failing BOOLEAN NOT NULL DEFAULT false,
	UNIQUE(consumer_id, topic_id)
);
CREATE INDEX ON subscriptions(tenant_id, consumer_id, topic_id);

-- webhooks_queue
CREATE TABLE webhook_events (
  id      	UUID NOT NULL,
  state     VARCHAR(16) NOT NULL,
	tenant_id				UUID NOT NULL,
	consumer_id				UUID NOT NULL,
	topic_id				UUID NOT NULL,
	version_id			UUID NOT NULL,
	subscription_id			UUID NOT NULL,

	-- retry config
	try_count				SMALLINT NOT NULL DEFAULT 0,

	-- request
	request_headers 	TEXT,
	request_body 			TEXT,

	-- response
	url				        VARCHAR(1024),
	response_code			SMALLINT,
  response_headers	TEXT,
  response_body			TEXT,
  duration_ms				INT,


	created_at 	      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	partition_date    DATE DEFAULT CURRENT_DATE,
	updated_at 				TIMESTAMP WITH TIME ZONE,
	delivered_at    	TIMESTAMP WITH TIME ZONE
) PARTITION BY RANGE (partition_date);
CREATE INDEX on webhook_events (consumer_id, state) where state < 'S600';
CREATE INDEX on webhook_events (consumer_id, state) where state >= 'S600';
CREATE INDEX on webhook_events (id);
CREATE INDEX on webhook_events (subscription_id);

CREATE TABLE webhooks_queue (
	id  		BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	event_id 	UUID NOT NULL
);


CREATE TABLE webhooks_retries (
	id  		BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	event_id 	UUID NOT NULL,
	execute_at 	TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON webhooks_retries(execute_at);
