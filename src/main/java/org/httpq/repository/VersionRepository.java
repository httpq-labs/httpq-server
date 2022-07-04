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
import org.httpq.model.Version;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VersionRepository extends AbstractRepository<Version> {

  VersionRepository(SqlConnection conn) {
    super(conn);
  }

  public Future<Map<UUID, String>> asMap(UUID tenantId) {
    return list(tenantId).map(versions -> {
      Map<UUID, String> map = new HashMap<>();
      for(Version version: versions) {
        map.put(version.id(), version.version());
      }
      return map;
    });
  }

  public Future<List<Version>> list(UUID tenantId) {
    return list(
      "SELECT id, tenant_id, version, created_at FROM versions WHERE tenant_id = $1",
      Tuple.of(tenantId),
      row -> new Version(
        row.getUUID("id"),
        row.getUUID("tenant_id"),
        row.getString("version"),
        row.getOffsetDateTime("created_at")));
  }

  public Future<UUID> create(UUID tenantId, String version) {
    UUID id = UUID.randomUUID();
    return create(
      "INSERT INTO versions (id, tenant_id, version) VALUES ($1, $2, $3)",
      Tuple.of(id, tenantId, version)).map(id);
  }
}
