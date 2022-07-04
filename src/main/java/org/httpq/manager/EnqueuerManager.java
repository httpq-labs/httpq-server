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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.httpq.exception.ValidationException;
import org.httpq.lib.Pair;
import org.httpq.model.*;
import org.httpq.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class EnqueuerManager {
  private final static Logger LOGGER = LoggerFactory.getLogger(EnqueuerManager.class);
  private final Function<SqlConnection, Repositories> servicesFactory;
  private final PgPool pool;

  public EnqueuerManager(PgPool pool, Function<SqlConnection, Repositories> servicesFactory) {
    this.servicesFactory = servicesFactory;
    this.pool = pool;
  }

  Future<UUID> enqueueNewWebhook(SqlConnection conn, NewWebhookEvent w) {
    Repositories repositories = servicesFactory.apply(conn);
    Set<String> receivedVersions = w.versionedPayload().keySet();

    VersionRepository versionService = repositories.version();
    TopicRepository topicService = repositories.topic();
    SubscriptionRepository subscriptionService = repositories.subscription();
    ConsumerRepository consumerService = repositories.consumer();
    WebhookEventRepository webhookEventService = repositories.webhookEvent();
    MainQueueRepository mainQueueService = repositories.mainQueue();

    // validate versions sent by the client map exactly the tenant versions
    Future<VersionsMap> validateVersionedPayload = versionService
      .asMap(w.tenantId())
      .map(tenantVersions -> {
        if (!new HashSet<>(tenantVersions.values()).equals(receivedVersions)) {
          LOGGER.error("tenant versions: {}, received versions: {}", tenantVersions.values(), receivedVersions);
          throw new ValidationException("invalid version");
        }
        return new VersionsMap(tenantVersions);
      });

    Future<Topic> validateTopic = topicService.findByName(w.tenantId(), w.topicName());

    return CompositeFuture
      .all(List.of(validateVersionedPayload, validateTopic))
      .map(res -> new Pair<VersionsMap, Topic>(res.resultAt(0), res.resultAt(1)))
      .flatMap(res -> {
        final VersionsMap versionsMap = res.e1();
        final Topic topic = res.e2();
        return CompositeFuture.all(
          List.of(
            subscriptionService.resolve(w.tenantId(), w.consumerId(), topic.id()),
            consumerService.get(w.consumerId())
          )).flatMap(compositeFuture -> {
          Subscription subscription = compositeFuture.resultAt(0);
          Consumer consumer = compositeFuture.resultAt(1);
          LOGGER.info("consumer {}", consumer);
          LOGGER.info("sub {}", subscription);
          JsonObject candidatePayload = w.versionedPayload().get(versionsMap.get(consumer.versionId()));

          return webhookEventService.create(
              w.tenantId(), w.consumerId(), topic.id(),
              consumer.versionId(), subscription.id(), candidatePayload)
            .flatMap(mainQueueService::enqueue);
        });
      });
  }

  public Future<UUID> enqueueNewWebhook(NewWebhookEvent newWebhookEvent) {
    return pool.withTransaction(conn -> enqueueNewWebhook(conn, newWebhookEvent));
  }
}
