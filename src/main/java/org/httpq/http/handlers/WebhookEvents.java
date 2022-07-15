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
package org.httpq.http.handlers;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import org.httpq.manager.EnqueuerManager;
import org.httpq.model.NewWebhookEvent;
import org.httpq.model.SecurityKey;
import org.httpq.model.SecurityKeyScope;
import org.httpq.repository.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WebhookEvents extends HttpHandler {
  private final EnqueuerManager enqueuerManager;
  private final PgPool pool;
  private final static Logger LOGGER = LoggerFactory.getLogger(WebhookEvents.class);
  public WebhookEvents(PgPool pool, EnqueuerManager enqueuerManager) {
    this.enqueuerManager = enqueuerManager;
    this.pool = pool;
  }

  @Override
  Set<SecurityKeyScope> getRequiredScope() {
    return Set.of(SecurityKeyScope.TENANT_USER);
  }

  public void get(RoutingContext ctx) {
    SecurityKey securityKey = authorize(ctx);
    UUID id = UUID.fromString(ctx.pathParam("eventId"));

    pool.withConnection(conn -> new Repositories(conn).webhookEvent().get(securityKey.tenantId(), id).onComplete(ar -> {
      if (ar.succeeded()) {
        ctx.response()
          .setStatusCode(200)
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(JsonObject.mapFrom(ar.result()).encodePrettily());
      } else {
        ctx.fail(ar.cause());
      }
    }));
  }

  public void create(RoutingContext ctx) {
    SecurityKey securityKey = authorize(ctx);
    JsonObject body = ctx.getBodyAsJson();
    UUID consumerId = UUID.fromString(body.getString("consumerId"));
    String topicId = body.getString("topic");
    JsonObject payload = body.getJsonObject("versionedPayloads");
    Map<String, JsonObject> payloads = new HashMap<>();

    for (String k : payload.fieldNames()) {
      payloads.put(k, payload.getJsonObject(k));
    }

    //TODO returns 404 if topic or versions not found
    enqueuerManager.enqueueNewWebhook(new NewWebhookEvent(securityKey.tenantId(), consumerId, topicId, payloads))
      .onComplete(ar -> {
        if (ar.succeeded()) {
          ctx.response()
            .setStatusCode(201)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(new JsonObject().put("id", ar.result()).encodePrettily());
        } else {
          ctx.fail(ar.cause());
        }
      });
  }

}
