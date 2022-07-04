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
import org.httpq.model.Consumer;

import java.util.List;
import java.util.UUID;

public class ConsumerRepository extends AbstractRepository<Consumer> {

  ConsumerRepository(SqlConnection conn) {
    super(conn);
  }


  public Future<List<Consumer>> list(UUID tenantId) {
    return list(
      "SELECT id, tenant_id, external_id, version_id, created_at FROM consumers WHERE tenant_id = $1",
      Tuple.of(tenantId),
      this::rowMapper
    );
  }

  public Consumer rowMapper(Row row) {
    return new Consumer(
      row.getUUID("id"),
      row.getUUID("tenant_id"),
      row.getString("external_id"),
      row.getUUID("version_id"),
      row.getOffsetDateTime("created_at"));
  }

  public Future<Consumer> get(UUID uuid) {
    return find(
      "SELECT id, tenant_id, external_id, version_id, created_at FROM consumers WHERE id = $1",
      Tuple.of(uuid),
      this::rowMapper
    );
  }

  public Future<UUID> create(UUID tenantId, String externalId, UUID apiVersionId) {
    UUID id = UUID.randomUUID();
    return create(
      "INSERT INTO consumers (id, tenant_id, external_id, version_id) VALUES ($1, $2, $3, $4)",
      Tuple.of(id, tenantId, externalId, apiVersionId)).map(id);
  }
}
