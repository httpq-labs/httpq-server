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
import org.httpq.model.SecurityKey;
import org.httpq.model.SecurityKeyScope;
import org.httpq.repository.Repositories;

import java.util.Set;
import java.util.UUID;

public class Subscriptions extends HttpHandler {
  private final PgPool pool;

  @Override
  Set<SecurityKeyScope> getRequiredScope() {
    return Set.of(SecurityKeyScope.TENANT_USER);
  }

  public Subscriptions(PgPool pool) {
    this.pool = pool;
  }

  public void list(RoutingContext ctx) {
    SecurityKey securityKey = authorize(ctx);

    pool.withConnection(conn -> new Repositories(conn).subscription().list(securityKey.tenantId()).onComplete(ar -> {
      if (ar.succeeded()) {
        JsonObject items = new JsonObject();
        items.put("items", ar.result().stream().map(JsonObject::mapFrom).toList());
        ctx.response().setStatusCode(200).end(items.encodePrettily());
      } else {
        ctx.fail(ar.cause());
      }
    }));
  }

  public void create(RoutingContext ctx) {
    SecurityKey securityKey = authorize(ctx);

    JsonObject o = ctx.getBodyAsJson();

    UUID consumerId = UUID.fromString(o.getString("consumerId"));
    UUID topicId = UUID.fromString(o.getString("topicId"));
    String url = o.getString("url");

    pool.withConnection(conn -> new Repositories(conn).subscription().create(securityKey.tenantId(), consumerId, topicId, url).onComplete(ar -> {
      if (ar.succeeded()) {
        ctx.response()
          .setStatusCode(201)
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end(new JsonObject().put("id", ar.result()).encodePrettily());
      } else {
        ctx.fail(ar.cause());
      }
    }));
  }

  public void update(RoutingContext ctx) {
    SecurityKey securityKey = authorize(ctx);

    JsonObject o = ctx.getBodyAsJson();

    UUID id = UUID.fromString(ctx.pathParam("subscriptionId"));
    String url = o.getString("url");

    pool.withConnection(conn -> new Repositories(conn).subscription().updateUrl(securityKey.tenantId(), id, url).onComplete(ar -> {
      if (ar.succeeded()) {
        ctx.response()
          .setStatusCode(200)
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .end();
      } else {
        ctx.fail(ar.cause());
      }
    }));
  }
}
