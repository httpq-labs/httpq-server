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
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.httpq.exception.ValidationException;
import org.httpq.model.Consumer;
import org.httpq.model.NewWebhookEvent;
import org.httpq.model.Subscription;
import org.httpq.model.Topic;
import org.httpq.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class EnqueuerManagerTest {

  @Mock
  PgPool pool;
  @Mock
  SqlConnection conn = mock(SqlConnection.class);
  @Mock
  VersionRepository versionService;
  @Mock
  TopicRepository topicService;
  @Mock
  SubscriptionRepository subscriptionService;
  @Mock
  ConsumerRepository consumerService;
  @Mock
  WebhookEventRepository webhookEventService;

  @Mock
  MainQueueRepository mainQueueService;

  EnqueuerManager enqueuerManager;

  UUID tenantId = UUID.randomUUID();
  UUID consumerId = UUID.randomUUID();
  UUID topicId = UUID.randomUUID();
  String topicName = "user.created";
  Topic topic = new Topic(topicId, tenantId, topicName, OffsetDateTime.now());
  String v1 = "v1";
  String v2 = "v2";
  UUID v1Id = UUID.randomUUID();
  UUID v2Id = UUID.randomUUID();
  Map<UUID, String> versionsMap = Map.of(v1Id, v1, v2Id, v2);
  UUID subscriptionId = UUID.randomUUID();
  String subscriptionUrl = "https://myurl.net/_wh";
  Subscription subscription = new Subscription(subscriptionId, tenantId, consumerId, topicId, subscriptionUrl, OffsetDateTime.now(), null, true, false);
  Consumer consumer = new Consumer(consumerId, tenantId, "432", v1Id, OffsetDateTime.now());

  @BeforeEach
  public void setup() {
    StaticRepositories.Builder b = new StaticRepositories.Builder();
    b.setVersionService(versionService);
    b.setTopicService(topicService);
    b.setSubscriptionService(subscriptionService);
    b.setConsumerService(consumerService);
    b.setWebhookEventService(webhookEventService);
    b.setMainQueueService(mainQueueService);
    StaticRepositories services = b.build();

    enqueuerManager = new EnqueuerManager(pool, (conn) -> services);
  }

  @Test
  public void test_enqueue_success(VertxTestContext tc) {
    JsonObject v1Payload = new JsonObject().put("hello", "world");
    JsonObject v2Payload = new JsonObject().put("hallow", "welt");
    Map<String, JsonObject> versionedPayloads = Map.of(v1, v1Payload, v2, v2Payload);

    NewWebhookEvent newWebhookEvent = new NewWebhookEvent(tenantId, consumerId, topicName, versionedPayloads);

    when(versionService.asMap(eq(tenantId))).thenReturn(Future.succeededFuture(versionsMap));
    when(topicService.findByName(eq(tenantId), eq(topicName))).thenReturn(Future.succeededFuture(topic));
    when(subscriptionService.resolve(eq(tenantId), eq(consumerId), eq(topicId))).thenReturn(Future.succeededFuture(subscription));
    when(consumerService.get(eq(consumerId))).thenReturn(Future.succeededFuture(consumer));
    UUID newWebhookId = UUID.randomUUID();
    when(webhookEventService.create(eq(tenantId), eq(consumerId), eq(topicId), eq(v1Id), eq(subscriptionId), eq(v1Payload))).thenReturn(Future.succeededFuture(newWebhookId));
    when(mainQueueService.enqueue(eq(newWebhookId))).thenReturn(Future.succeededFuture(newWebhookId));

    enqueuerManager.enqueueNewWebhook(conn, newWebhookEvent).onComplete(tc.succeeding(createdUuid -> {
      assertEquals(newWebhookId, createdUuid);
      tc.completeNow();
    }));
  }

  @Test
  public void test_enqueue_bad_versioned_payloads(VertxTestContext tc) {
    JsonObject v1Payload = new JsonObject().put("hello", "world");

    Map<String, JsonObject> versionedPayloads = Map.of(v1, v1Payload);

    NewWebhookEvent  newWebhookEvent = new NewWebhookEvent(tenantId, consumerId, topicName, versionedPayloads);
    when(topicService.findByName(eq(tenantId), eq(topicName))).thenReturn(Future.succeededFuture(topic));
    when(versionService.asMap(eq(tenantId))).thenReturn(Future.succeededFuture(versionsMap));

    enqueuerManager.enqueueNewWebhook(conn, newWebhookEvent).onComplete(tc.failing(ex -> {
      assertTrue(ex instanceof ValidationException);
      tc.completeNow();
    }));
  }
}
