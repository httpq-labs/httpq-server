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

import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.flywaydb.core.Flyway;
import org.httpq.db.Database;
import org.httpq.db.FlywayFactory;
import org.httpq.model.TypedConf;
import org.httpq.server.HttpApiFixtures;
import org.httpq.server.MainVerticle;
import org.httpq.server.TestData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.port;

@ExtendWith(VertxExtension.class)
public class QueueTests {

  static TypedConf typedConf;
  static HttpApiFixtures httpApiFixtures;
  static PgPool pool;

  static Flyway flyway;
  static EventBus eb;
  static TestData testData;

  @BeforeAll
  static void beforeAll(Vertx vertx, VertxTestContext testContext) {
    typedConf = TypedConf.load();
    flyway = FlywayFactory.make(typedConf);
    flyway.clean();
    flyway.migrate();

    pool = new Database(vertx).getSharedPool();
    httpApiFixtures = new HttpApiFixtures(typedConf);
    eb = vertx.eventBus();

    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> {
      testData = httpApiFixtures.setupTestData();
      testContext.completeNow();
    }));
  }



  @BeforeEach
  public void beforeEach() {
    RestAssured.reset();
    //RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    baseURI = "http://%s".formatted(typedConf.httpHost());
    port = typedConf.httpPort();

  }

  @AfterAll
  static void afterAll(Vertx vertx) {
    pool.close().onSuccess(s -> vertx.close());
  }


  @Test
  public void boo(){

  }
//
//  @Test
//  @Timeout(value = 600, timeUnit = TimeUnit.SECONDS)
//  public void dequeue(Vertx vertx, VertxTestContext tc) {
//    MainQueueService mainQueue = new MainQueueService(pool);
//
//    WebhookEventService webhookEventService = new WebhookEventService(pool,
//      new ConsumerService(pool),
//      new SubscriptionService(pool),
//      new VersionService(pool),
//      new TopicService(pool),
//      new MainQueueService(pool),
//      new RetryQueueService(pool));
//
//    RetryQueueService retryQueue = new RetryQueueService(pool);
//    fixtures.updateSubscription(testData.userSpec(), testData.subscription(), fixtures.endpointUrl("/status/200,200,200,500"));
//
//    // 200s
//    int n200 = 35;
//
//    for (int i = 0; i < n200; i++) {
//      fixtures.createWebhook(testData.userSpec(), testData.consumer(), testData.topicStr(), testData.versionedPayload(), 201);
//    }
//
//    DequeuerManager deq = new DequeuerManager(pool, eb, mainQueue, retryQueue, webhookEventService);
//    deq.pollMainQueue().onComplete(tc.succeeding(h -> {
//
//      //fixtures.updateSubscription(testData.userSpec(), subscription500, fixtures.endpointUrl("/status/200,200,200,500"));
//
//      deq.pollRetryQueue(OffsetDateTime.now()).onComplete(tc.succeedingThenComplete());
//
//    }));
//
//  }
//
//
//  @Test
//  void main_queue_enqueue_dequeue(VertxTestContext testContext) {
//    MainQueueService mainQueue = new MainQueueService(pool);
//    EventGenerator<UUID> events = new EventGenerator<>(UUID::randomUUID);
//    List<Future> enqueued = new ArrayList<>();
//    int n = 107;
//    for (int i = 0; i < n; i++) {
//      enqueued.add(mainQueue.enqueue(events.make()));
//    }
//    List<UUID> dequeued = new ArrayList<>();
//
//    CompositeFuture.all(enqueued).onComplete(testContext.succeeding(h -> {
//      mainQueue.dequeue(conn).map(firstBatch -> {
//        dequeued.addAll(firstBatch);
//        if (firstBatch.size() != 100) {
//          testContext.failNow("invalid batching");
//        }
//        return Future.succeededFuture();
//      }).onComplete(testContext.succeeding(h2 -> {
//        mainQueue.dequeue(conn).map(secondBatch -> {
//          if (secondBatch.size() != 7) {
//            testContext.failNow("invalid batching");
//          }
//          dequeued.addAll(secondBatch);
//          return Future.succeededFuture();
//        }).onComplete(testContext.succeeding(h3 -> {
//          if (!new HashSet<>(dequeued).equals(events.getAll())) {
//            testContext.failNow("dequeued events don't match enqueued events");
//          } else {
//            testContext.completeNow();
//          }
//        }));
//      }));
//    }));
//  }
//
//  @Test
//  void retry_queue_enqueue_dequeue(VertxTestContext testContext) {
//    RetryQueueService retryQueueService = new RetryQueueService(pool);
//
//    List<Future> enqueued = new ArrayList<>();
//    OffsetDateTime now = OffsetDateTime.now();
//
//    UUID event1 = UUID.randomUUID();
//    UUID event2 = UUID.randomUUID();
//    UUID event3 = UUID.randomUUID();
//    UUID event4 = UUID.randomUUID();
//
//    enqueued.add(retryQueueService.enqueueForLater(event1, now.plus(Duration.ofSeconds(1))));
//    enqueued.add(retryQueueService.enqueueForLater(event2, now.plus(Duration.ofSeconds(2))));
//    enqueued.add(retryQueueService.enqueueForLater(event3, now.plus(Duration.ofSeconds(3))));
//    enqueued.add(retryQueueService.enqueueForLater(event4, now.plus(Duration.ofSeconds(4))));
//
//    CompositeFuture.all(enqueued).onComplete(testContext.succeeding(h -> {
//      retryQueueService.dequeueOlderThan( now.plus(Duration.ofSeconds(2)), uuids -> {
//        if (!new HashSet<>(uuids).equals(Set.of(event1, event2))) {
//          testContext.failNow("invalid dequeuing 1");
//        }
//        retryQueueService.dequeueOlderThan(now.plus(Duration.ofSeconds(4)), uuids2 -> {
//          if (!new HashSet<>(uuids2).equals(Set.of(event3, event4))) {
//            testContext.failNow("invalid dequeuing 2");
//          }
//          return Future.succeededFuture();
//        }).onComplete(testContext.succeeding(success -> testContext.completeNow()));
//
//        return Future.succeededFuture();
//      });
//
//
//
//    }));
//  }
}
