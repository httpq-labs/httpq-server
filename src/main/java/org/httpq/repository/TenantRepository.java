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
import org.httpq.model.Tenant;

import java.util.UUID;

public class TenantRepository extends AbstractRepository<Tenant> {

  TenantRepository(SqlConnection conn) {
    super(conn);
  }

  public Future<Tenant> get(UUID id) {
    return find("SELECT id, name, created_at FROM tenants WHERE id = $1",
      Tuple.of(id),
      row -> new Tenant(
        row.getUUID("id"),
        row.getString("name"),
        row.getOffsetDateTime("created_at"))
    );
  }

  public Future<UUID> create(String name) {
    UUID id = UUID.randomUUID();
    return create("INSERT INTO tenants (id, name) VALUES ($1, $2)",
      Tuple.of(id, name)).map(id);
  }
}
