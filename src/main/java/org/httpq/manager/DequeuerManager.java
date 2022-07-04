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
package org.httpq.manager;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.httpq.repository.MainQueueRepository;
import org.httpq.repository.Repositories;
import org.httpq.repository.RetryQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

public class DequeuerManager {
  private final static Logger LOGGER = LoggerFactory.getLogger(DequeuerManager.class);

  private final PgPool pool;
  private final Function<SqlConnection, Repositories> servicesFactory;
  private final Function<SqlConnection, WebhookSendPipeline> webhookManagerFactory;

  public DequeuerManager(PgPool pool, Function<SqlConnection, Repositories> servicesFactory, Function<SqlConnection, WebhookSendPipeline> webhookManagerFactory) {
    this.pool = pool;
    this.servicesFactory = servicesFactory;
    this.webhookManagerFactory = webhookManagerFactory;
  }

  public Future<Integer> pollMainQueue() {
    return pool.withTransaction(conn -> {
      MainQueueRepository mainQueueService = servicesFactory.apply(conn).mainQueue();
      return mainQueueService.dequeue().flatMap(uuids -> handlePolling(conn, uuids).map(uuids.size()));
    });
  }

  private Future<Void> handlePolling(SqlConnection conn, Collection<UUID> uuids) {
    return webhookManagerFactory.apply(conn).apply(uuids);
  }

  public Future<Integer> pollRetryQueue(OffsetDateTime dequeueOlderThan) {
    return pool.withTransaction(conn -> {
      RetryQueueRepository retryQueueService = new Repositories(conn).retryQueue();
      return retryQueueService.dequeueOlderThan(dequeueOlderThan).flatMap(uuids -> handlePolling(conn, uuids).map(uuids.size()));
    });
  }

}
