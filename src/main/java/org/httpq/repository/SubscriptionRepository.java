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
import org.httpq.model.Subscription;

import java.util.List;
import java.util.UUID;

//TODO One subscription -> multiple topics -> one url
public class SubscriptionRepository extends AbstractRepository<Subscription> {
  SubscriptionRepository(SqlConnection conn) {
    super(conn);
  }

  public Future<Void> disableAndFail(List<UUID> ids) {
    String sql = """
        UPDATE subscriptions
        SET
          is_active = false,
          is_failing = true,
          updated_at = NOW()
        WHERE id = ANY($1)
        """;
    return updateBulk(
      sql,
      Tuple.of(toArray(ids))
      );
  }

  public Future<List<UUID>> list(List<UUID> ids, boolean isActive, boolean isFailing) {
    return list(
      "SELECT id FROM subscriptions WHERE id = ANY($1) and is_active = $2 and is_failing = $3",
      Tuple.of(toArray(ids), isActive, isFailing),
      row -> row.getUUID("id")
    );
  }

  public Future<List<Subscription>> list(UUID tenantId) {
    return list(
      "SELECT id, tenant_id, consumer_id, topic_id, url, created_at, updated_at, is_active, is_failing FROM subscriptions WHERE tenant_id = $1",
      Tuple.of(tenantId),
      row -> new Subscription(
        row.getUUID("id"),
        row.getUUID("tenant_id"),
        row.getUUID("consumer_id"),
        row.getUUID("topic_id"),
        row.getString("url"),
        row.getOffsetDateTime("created_at"),
        row.getOffsetDateTime("updated_at"),
        row.getBoolean("is_active"),
        row.getBoolean("is_failing")
      )
    );
  }

  public Future<UUID> updateUrl(UUID tenantId, UUID id, String url) {
    String sql = """
        UPDATE subscriptions
        SET
          url = $3,
          updated_at = NOW()
        WHERE id = $1 and tenant_id = $2
        """;
    return update(
      sql,
      List.of(Tuple.of(id, tenantId, url))
    ).map(id);
  }

  public Future<UUID> updateState(UUID tenantId, UUID id, boolean isActive, boolean isFailing) {
    String sql = """
        UPDATE subscriptions
        SET
          is_active = $3,
          is_failing = $4,
          updated_at = NOW()
        WHERE id = $1 and tenant_id = $2
        """;
    return update(
      sql,
      List.of(Tuple.of(id, tenantId, isActive, isFailing))
    ).map(id);
  }
  public Future<Subscription> resolve(UUID tenantId, UUID consumerId, UUID topicId) {
    return list(
      """
        SELECT id, tenant_id, consumer_id, topic_id, url, created_at, updated_at, is_active, is_failing
        FROM subscriptions
        WHERE tenant_id = $1 AND consumer_id = $2 AND topic_id = $3
        LIMIT 1""",
      Tuple.of(tenantId, consumerId, topicId),
      this::rowMapper).map(list -> list.get(0));
  }

  Subscription rowMapper(Row row) {
    return new Subscription(
      row.getUUID("id"),
      row.getUUID("tenant_id"),
      row.getUUID("consumer_id"),
      row.getUUID("topic_id"),
      row.getString("url"),
      row.getOffsetDateTime("created_at"),
      row.getOffsetDateTime("updated_at"),
      row.getBoolean("is_active"),
      row.getBoolean("is_failing")
    );
  }

  public Future<Subscription> get(UUID id) {
    return find("SELECT id, tenant_id, consumer_id, topic_id, url, created_at, updated_at, is_active, is_failing FROM subscriptions WHERE id = $1",
      Tuple.of(id),
      this::rowMapper
    );
  }

  public Future<UUID> create(
    UUID tenantId,
    UUID consumerId,
    UUID topicId,
    String url
  ) {
    UUID id = UUID.randomUUID();
    return create("INSERT INTO subscriptions (id, tenant_id, consumer_id, topic_id, url, is_active) VALUES ($1, $2, $3, $4, $5, $6)",
      Tuple.of(id, tenantId, consumerId, topicId, url, true)).map(id);
  }

}
