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
package org.httpq.server;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import org.httpq.db.Database;
import org.httpq.db.Pools;
import org.httpq.injector.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;


public class RetryDequeuerVerticle extends AbstractDequeuerVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(RetryDequeuerVerticle.class);

  public Future<Void> dequeue() {
    PgPool pool = new Database(vertx).getDedicatedPool(Pools.RETRY_DQR);
    return new Injector().getDequeuerManager(pool, vertx.eventBus()).pollRetryQueue(OffsetDateTime.now(Clock.systemUTC()))
      .onSuccess(i -> LOGGER.info("dequeued {} retry events", i))
      .onFailure(ex -> LOGGER.error("failure to dequeue retry events", ex))
      .mapEmpty();
  }
}
