/*
 * httpq - the webhooks sending server
 * Copyright © 2022 Edward Swiac (eswiac@fastmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.httpq.repository;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.httpq.model.ScheduledWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class RetryQueueRepository extends AbstractRepository<UUID> {
  private final static Logger LOGGER = LoggerFactory.getLogger(RetryQueueRepository.class);
  RetryQueueRepository(SqlConnection conn) {
    super(conn);
  }


  public Future<Void> enqueue(List<ScheduledWebhook> webhooks) {
    if (webhooks.isEmpty()) {
      return Future.succeededFuture();
    }
    LOGGER.info("retrying {}", webhooks);
    return createBatch("INSERT INTO webhooks_retries (event_id, execute_at) VALUES ($1, $2)",
      webhooks.stream().map(w -> Tuple.of(w.eventId(), w.executeAt())).toList());
  }

  public Future<List<UUID>> dequeueOlderThan(OffsetDateTime time) {
    return list("""
        DELETE FROM webhooks_retries
                    USING (SELECT id, event_id, execute_at FROM webhooks_retries WHERE execute_at <= $1 LIMIT 100 FOR UPDATE SKIP LOCKED) q
                    WHERE q.id = webhooks_retries.id
                    RETURNING webhooks_retries.*;
          """,
      Tuple.of(time),
      row -> row.getUUID("event_id"));
  }
}
