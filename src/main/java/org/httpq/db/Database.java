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
package org.httpq.db;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.flywaydb.core.Flyway;
import org.httpq.model.TypedConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
  private final Vertx vertx;
  private final TypedConf config;

  private final static Logger LOGGER = LoggerFactory.getLogger(Database.class);

  public Database(Vertx vertx) {
    this.vertx = vertx;
    this.config = TypedConf.load();
  }

  public PgPool getPool(int poolSize, String name, boolean isShared) {
    LOGGER.info("requesting pg pool size={} name={} shared={}", poolSize, name, isShared);
    PoolOptions poolOptions = new PoolOptions()
      .setName(name)
      .setShared(isShared)
      .setMaxSize(poolSize);

    PgConnectOptions connectOptions = PgConnectOptions.fromUri(config.dbUrl())
      .setCachePreparedStatements(true)
      .setReconnectAttempts(3)
      .setReconnectInterval(1000);

    return PgPool.pool(vertx, connectOptions, poolOptions);
  }

  public PgPool getSharedPool() {
    return getPool(3, Pools.SHARED, true);
  }

  public PgPool getDedicatedPool(String poolName) {
    return getPool(1, poolName, true);
  }

  public Future<Void> migrateBlocking() {
    return vertx.executeBlocking(h -> {
      try {
        Flyway flyway = FlywayFactory.make(config);
        flyway.migrate();
        flyway.getConfiguration().getDataSource().getConnection().close();
        h.complete();
      } catch (Exception ex) {
        h.fail(ex);
      }
    });
  }

}
