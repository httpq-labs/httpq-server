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
import org.httpq.exception.ValidationException;
import org.httpq.model.SecurityKeyScope;
import org.httpq.repository.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

public class Tenants extends HttpHandler {
  private final static Logger LOGGER = LoggerFactory.getLogger(Tenants.class);
  private final PgPool pool;

  @Override
  Set<SecurityKeyScope> getRequiredScope() {
    return Set.of(SecurityKeyScope.ROOT);
  }

  public Tenants(PgPool pool) {
    this.pool = pool;
  }

  public void get(RoutingContext ctx) {
    authorize(ctx);

    String uuidStr = ctx.pathParam("tenantId");
    if (uuidStr == null || uuidStr.isEmpty()) {
      ctx.fail(new ValidationException("missing required identifier"));
      return;
    }
    UUID uuid;
    try {
      uuid = UUID.fromString(uuidStr);
    } catch (IllegalArgumentException illegalArgumentException) {
      ctx.fail(new ValidationException("invalid identifier format"));
      return;
    }

    pool.withConnection(conn -> new Repositories(conn).tenant().get(uuid).onComplete(ar -> {
      if (ar.succeeded()) {
        ctx.response().end(ar.result().toJson().encode());
      } else {
        LOGGER.error("error", ar.cause());
        ctx.fail(500);
      }
    }));
  }

  public void create(RoutingContext ctx) {
    authorize(ctx);

    JsonObject o = ctx.getBodyAsJson();

    String name = o.getString("name");

    pool.withConnection(conn -> new Repositories(conn).tenant().create(name).onComplete(ar -> {
      if (ar.succeeded()) {
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
          .setStatusCode(201).end(new JsonObject().put("id", ar.result()).encodePrettily());
      } else {
        ctx.fail(ar.cause());
      }
    }));

  }


}
