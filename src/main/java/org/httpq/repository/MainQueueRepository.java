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

import java.util.List;
import java.util.UUID;

public class MainQueueRepository extends AbstractRepository<UUID> {
  MainQueueRepository(SqlConnection conn) {
    super(conn);
  }

  public Future<UUID> enqueue(UUID eventId) {
    return create("INSERT INTO webhooks_queue (event_id) VALUES ($1)",
      Tuple.of(eventId)).map(eventId);
  }

  public Future<List<UUID>> dequeue() {
    return dequeue(200);
  }

  public Future<List<UUID>> dequeue(int dequeueCount) {
    return list(
      """
           DELETE FROM webhooks_queue
           USING (SELECT id, event_id FROM webhooks_queue LIMIT $1 FOR UPDATE SKIP LOCKED) q
           WHERE q.id = webhooks_queue.id
           RETURNING webhooks_queue.*;
        """,
      Tuple.of(dequeueCount),
      row -> row.getUUID("event_id"));
  }
}
