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
import org.httpq.exception.UnauthorizedException;
import org.httpq.exception.ValidationException;
import org.httpq.model.SecurityKey;
import org.httpq.model.SecurityKeyScope;

import java.util.Set;

public abstract class HttpHandler {

  Set<SecurityKeyScope> getRequiredScope() {
    return Set.of();
  }

  private SecurityKey getSecurityKey(RoutingContext ctx) {
    JsonObject apiKey = ctx.user().get("security_key");
    return apiKey.mapTo(SecurityKey.class);
  }

  protected SecurityKey authorize(RoutingContext ctx) throws UnauthorizedException {
    SecurityKey securityKey = getSecurityKey(ctx);
    if (!getRequiredScope().contains(securityKey.scope())) {
      throw new UnauthorizedException();
    }
    return securityKey;
  }

  protected void validate(boolean cond, String message) throws ValidationException {
    if (!cond) {
      throw new ValidationException(message);
    }
  }

  protected void validate(boolean cond) throws ValidationException {
    validate(cond, null);
  }

}
