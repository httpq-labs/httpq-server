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


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgPool;
import org.httpq.db.Database;
import org.httpq.db.Pools;
import org.httpq.repository.DailyPartitionTableRepository;
import org.httpq.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Random;

public class PartitionManagerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(PartitionManagerVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Random random = new Random();
    Duration initialJitter = Duration.ofMillis(random.nextInt(1, 1000));
    Duration executionInterval = Duration.ofMinutes(random.nextInt(60, 3 * 24 * 60));
    LOGGER.info("initial jitter is {}, execution interval is {}", initialJitter, executionInterval);
    vertx.setTimer(initialJitter.toMillis(), h -> {
      execute().onSuccess(_res -> {
        vertx.setPeriodic(executionInterval.toMillis(), hh -> execute());
      }).onSuccess(suc -> startPromise.complete());
    });
  }

  public Future<Void> execute() {
    PgPool pool = new Database(vertx).getPool(1, Pools.PARTMAN, false);
    return pool.withTransaction(conn ->
      new DailyPartitionTableRepository(conn, WebhookEventRepository.TABLE_NAME, LocalDate.now(Clock.systemUTC()))
      .reconcile()
      .onFailure(ex -> LOGGER.error("partition reconciliation error", ex))).eventually(ev->pool.close()).mapEmpty();
  }


}
