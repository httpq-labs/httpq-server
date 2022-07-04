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
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.httpq.model.*;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WebhookEventRepository extends AbstractRepository<WebhookEvent> {
  public static final String TABLE_NAME = "webhook_events";

  private final static Logger LOGGER = LoggerFactory.getLogger(WebhookEventRepository.class);
  private final LocalDate partitionKey;
  private final LocalDate pruningParameter;

  WebhookEventRepository(SqlConnection conn) {
    this(conn, LocalDate.now(Clock.systemUTC()));
  }

  @VisibleForTesting
  WebhookEventRepository(SqlConnection conn, LocalDate partitionKey) {
    super(conn);
    this.partitionKey = partitionKey;
    this.pruningParameter = partitionKey.minusDays(2);
  }

  public Future<List<AttemptedWebhook>> recordWebhookAttempt(Collection<WebhookHttpResponse> webhookForUpdates) {
    if (webhookForUpdates.isEmpty()) {
      return Future.succeededFuture(List.of());
    }
    String sql = """
      UPDATE %s
      SET
        state = $2,
        try_count = try_count + 1,
        response_code = $3,
        response_body = $4,
        duration_ms = $5,
        url = $6,
        updated_at = NOW(),
        delivered_at = $7
      WHERE id = $1
      AND partition_date >= $8
      RETURNING id, state, subscription_id, try_count, created_at;
      """.formatted(TABLE_NAME);
    return updateReturning(
      sql,
      webhookForUpdates.stream().map(w -> Tuple.of(w.eventId(), w.state(), w.statusCode(), w.body(), w.duration().toMillis(), w.url(), w.deliveredAt(), pruningParameter)).toList(),
      row -> new AttemptedWebhook(
        row.getUUID("id"),
        WebhookState.valueOf(row.getString("state")),
        row.getUUID("subscription_id"),
        row.getInteger("try_count"),
        row.getOffsetDateTime("created_at")
        )
    );
  }

  public Future<List<WebhookHttpRequest>> listForSending(final Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return Future.succeededFuture(List.of());
    }
    return list("""
        SELECT
        we.id, we.state, we.request_headers, we.request_body, s.url
        FROM %s we
        INNER JOIN subscriptions s on s.id = we.subscription_id
        and s.is_active = true and s.is_failing = false
        WHERE we.id = ANY($1)
        AND we.partition_date >= $2
        """.formatted(TABLE_NAME),
      Tuple.of(toArray(ids), pruningParameter),
      row -> WebhookHttpRequest.make(
        row.getUUID("id"),
        row.getString("url"),
        row.getString("request_body")
      ));
  }

  public Future<List<WebhookEvent>> list(final List<UUID> ids) {
    if (ids.isEmpty()) {
      return Future.succeededFuture(List.of());
    }
    return list("""
        SELECT
        we.id, we.state, we.tenant_id, we.consumer_id, we.topic_id,
        we.version_id, we.subscription_id, we.try_count,
        we.request_headers, we.request_body,
        we.url, s.url as sub_url, we.response_code,
        we.response_headers, we.response_body, we.duration_ms,
        we.created_at, we.updated_at, we.delivered_at
        FROM %s we
        INNER JOIN subscriptions s on s.id = we.subscription_id
        WHERE we.id = ANY($1)
        AND  we.partition_date >= $2
        """.formatted(TABLE_NAME),
      Tuple.of(toArray(ids), pruningParameter),
      this::rowMapper
    );
  }

  public Future<WebhookEvent> get(final UUID tenantId, final UUID id) {
    return find("""
        SELECT
        id, state, tenant_id, consumer_id, topic_id,
        version_id, subscription_id, try_count,
        request_headers, request_body,
        url, response_code,
        response_headers, response_body, duration_ms,
        created_at, updated_at, delivered_at
        FROM %s
        WHERE id = $1
        AND partition_date >= $2
        """.formatted(TABLE_NAME),
      Tuple.of(id, pruningParameter),
      this::rowMapper
    );
  }


  private WebhookEvent rowMapper(Row row) {
    return new WebhookEvent(
      row.getUUID("id"),
      WebhookState.valueOf(row.getString("state")),
      row.getUUID("tenant_id"),
      row.getUUID("consumer_id"),
      row.getUUID("topic_id"),
      row.getUUID("version_id"),
      row.getUUID("subscription_id"),
      row.getInteger("try_count"),
      Headers.deserialize(row.getString("request_headers")),
      Optional.ofNullable(row.getString("request_body")).map(JsonObject::new).orElse(null),
      row.getString("url"),
      Headers.deserialize(row.getString("response_headers")),
      row.getString("response_body"),
      row.getInteger("response_code"),
      Optional.ofNullable(row.getInteger("duration_ms")).map(Duration::ofMillis).orElse(null),
      row.getOffsetDateTime("created_at"),
      row.getOffsetDateTime("updated_at"),
      row.getOffsetDateTime("delivered_at"));
  }

  public Future<UUID> create(
    final UUID tenantId,
    final UUID consumerId,
    final UUID topicID,
    final UUID versionId,
    final UUID subscriptionId,
    final JsonObject payload) {
    UUID id = UUID.randomUUID();

    return create( """
                INSERT INTO %s (
                id, state, tenant_id, consumer_id, topic_id,
                version_id, subscription_id, request_body, partition_date)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)""".formatted(TABLE_NAME),
      Tuple.of(
        id, WebhookState.S000, tenantId, consumerId, topicID,
        versionId, subscriptionId, payload.encode(), partitionKey))
      .map(id);
  }
}
