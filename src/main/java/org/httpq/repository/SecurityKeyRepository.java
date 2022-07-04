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
import org.httpq.exception.ValidationException;
import org.httpq.model.OneTimeSecurityKey;
import org.httpq.model.SecurityKey;
import org.httpq.model.SecurityKeyScope;

import java.util.UUID;

public class SecurityKeyRepository extends AbstractRepository<SecurityKey> {

  SecurityKeyRepository(SqlConnection conn) {
    super(conn);
  }

  public Future<SecurityKey> getBySecurityKey(String apiKey) {
    try {
      return getBySecurityKey(UUID.fromString(apiKey));
    } catch (IllegalArgumentException ex) {
      return Future.failedFuture(new ValidationException("Invalid API key"));
    }
  }

  public Future<SecurityKey> getBySecurityKey(UUID apiKey) {
    return find(
      "SELECT id, security_key, scope, parent_key, tenant_id, created_at FROM security_keys WHERE security_key = $1",
      Tuple.of(apiKey),
      row -> new SecurityKey(
        row.getUUID("id"),
        SecurityKeyScope.valueOf(row.getString("scope")),
        row.getUUID("parent_key"),
        row.getUUID("tenant_id"),
        row.getOffsetDateTime("created_at")
      ));
  }

  public Future<OneTimeSecurityKey> create(SecurityKeyScope securityKeyScope, UUID parentKey, UUID tenantId) {
    UUID id = UUID.randomUUID();
    UUID securityKey = UUID.randomUUID();

    return create(
      "INSERT INTO security_keys (id, scope, parent_key, tenant_id, security_key) VALUES ($1, $2, $3, $4, $5)",
      Tuple.of(id, securityKeyScope, parentKey, tenantId, securityKey))
      .map(new OneTimeSecurityKey(id, securityKey, securityKeyScope, tenantId));
  }
}
