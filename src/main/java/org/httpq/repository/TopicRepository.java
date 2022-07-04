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
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.httpq.model.Topic;

import java.util.List;
import java.util.UUID;

public class TopicRepository extends AbstractRepository<Topic> {

  TopicRepository(SqlConnection conn) {
    super(conn);
  }

  public Future<Topic> findByName(UUID tenantId, String topicName) {
    return find(
      """
        SELECT id, tenant_id, name, created_at
        FROM topics
        WHERE tenant_id = $1 AND name = $2
        LIMIT 1""",
      Tuple.of(tenantId, topicName),
      this::rowMapper);
  }

  public Topic rowMapper(Row row) {
    return new Topic(
      row.getUUID("id"),
      row.getUUID("tenant_id"),
      row.getString("name"),
      row.getOffsetDateTime("created_at")
    );
  }
  public Future<List<Topic>> list(UUID tenantId) {
    return list("SELECT id, tenant_id, name, created_at FROM topics WHERE tenant_id = $1",
      Tuple.of(tenantId),
     this::rowMapper);
  }

  public Future<Topic> get(UUID id) {
    return find(
      "SELECT id, tenant_id, name, created_at FROM topics WHERE id = $1",
      Tuple.of(id),
      this::rowMapper
    );
  }

  public Future<UUID> create(UUID tenantId, String name) {
    UUID id = UUID.randomUUID();
    return create("INSERT INTO topics (id, tenant_id, name) VALUES ($1, $2, $3)",
      Tuple.of(id, tenantId, name)).map(id);
  }
}
