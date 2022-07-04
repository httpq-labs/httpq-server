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

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import org.httpq.exception.UnauthorizedException;
import org.httpq.model.SecurityKey;
import org.httpq.model.SecurityKeyScope;
import org.httpq.repository.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

public class SecurityKeys extends HttpHandler {
  private final static Logger LOGGER = LoggerFactory.getLogger(Tenants.class);
  private final PgPool pool;

  @Override
  Set<SecurityKeyScope> getRequiredScope() {
    return Set.of(SecurityKeyScope.ROOT, SecurityKeyScope.TENANT_MASTER);
  }

  public SecurityKeys(PgPool pool) {
    this.pool = pool;
  }

  public void create(RoutingContext ctx) {
    SecurityKey callerKey = authorize(ctx);

    JsonObject o = ctx.getBodyAsJson();

    SecurityKeyScope childKeyType;
    UUID tenantId;

    switch (callerKey.scope()) {
      case ROOT -> {
        childKeyType = SecurityKeyScope.TENANT_MASTER;
        validate(o.getString("tenantId") != null, "a tenant is required");
        tenantId = UUID.fromString(o.getString("tenantId"));
      }
      case TENANT_MASTER -> {
        childKeyType = SecurityKeyScope.TENANT_USER;
        tenantId = callerKey.tenantId();
      }
      default -> throw new UnauthorizedException();
    }

    pool.withConnection(conn -> new Repositories(conn).securityKey().create(childKeyType, callerKey.id(), tenantId).onComplete(ar -> {
      if (ar.succeeded()) {
        ctx.response()
          .setStatusCode(201).end(JsonObject.mapFrom(ar.result()).encodePrettily());
      } else {
        ctx.fail(ar.cause());
      }
    }));
  }

}
