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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import org.httpq.model.Consumer;
import org.httpq.model.ScheduledWebhook;
import org.httpq.model.SecurityKeyScope;
import org.httpq.model.Subscription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class AllRepositoriesTest extends TestWithRepositoryFixtures {

  @Test
  public void test_tenant(VertxTestContext tc) {
    TenantRepository tenantService = new TenantRepository(conn);
    tenantService.get(tenantId).onComplete(tc.succeeding(t -> {
      assertEquals(t.name(), tenantName);
      tc.completeNow();
    }));
  }

  @Test
  public void test_consumer(VertxTestContext tc) {
    ConsumerRepository consumerService = new ConsumerRepository(conn);

    consumerService.create(tenantId, "123", versionId).onComplete(tc.succeeding(id -> {
      consumerService.get(id).onComplete(tc.succeeding(c -> {
        Assertions.assertEquals(c.id(), id);
        Assertions.assertEquals(c.versionId(), versionId);
        Assertions.assertEquals(c.tenantId(), tenantId);
        Assertions.assertEquals(c.externalId(), "123");
        Assertions.assertTrue(c.createdAt().isAfter(OffsetDateTime.now().minusSeconds(1)));

        // create another consumer and lookup both
          consumerService.list(tenantId).onComplete(tc.succeeding(consumers -> {
            assertEquals(consumers.size(), 2);
            Consumer c1 = consumers.get(0);
            Consumer c2 = consumers.get(1);
            Assertions.assertEquals(c, c1);
            assertEquals(c2.id(), consumerId);
            assertNotEquals(c1, c2);
            tc.completeNow();
          }));
      }));
    }));
  }

  @Test
  public void test_main_queue(VertxTestContext tc) {
    MainQueueRepository mainQueueService = new MainQueueRepository(conn);
    List<Future> insertions = new ArrayList<>();
    int n = 100;
    for(int i = 0; i < 206; i++) {
      insertions.add(mainQueueService.enqueue(UUID.randomUUID()));
    }

    CompositeFuture.all(insertions).onComplete(tc.succeeding(inserted -> {
      List<UUID> uuids = inserted.list();
      mainQueueService.dequeue(n).onComplete(tc.succeeding(firstBatch -> {
        assertThat(uuids.subList(0, 100)).containsExactlyInAnyOrderElementsOf(firstBatch);
        mainQueueService.dequeue(n).onComplete(tc.succeeding(secondBatch -> {
          assertThat(uuids.subList(100, 200)).containsExactlyInAnyOrderElementsOf(secondBatch);
          mainQueueService.dequeue(n).onComplete(tc.succeeding(thirdBatch -> {
            assertThat(uuids.subList(200, 206)).containsExactlyInAnyOrderElementsOf(thirdBatch);
            mainQueueService.dequeue(n).onComplete(tc.succeeding(_empty -> {
              assertEquals(_empty, List.of());
              tc.completeNow();
            }));
          }));
        }));
      }));
    }));
  }

  @Test
  public void test_retry_queue(VertxTestContext tc) {
    RetryQueueRepository retryQueueService = new RetryQueueRepository(conn);
    List<Future> insertions = new ArrayList<>();
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime t1 = now.minusMinutes(1);
    List<UUID> uuids1 = List.of(UUID.randomUUID(), UUID.randomUUID());
    OffsetDateTime t2 = now.plusMinutes(1);
    List<UUID> uuids2 = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    insertions.add(retryQueueService.enqueue(uuids1.stream().map(uuid -> new ScheduledWebhook(uuid, t1)).collect(Collectors.toList())));
    insertions.add(retryQueueService.enqueue(uuids2.stream().map(uuid -> new ScheduledWebhook(uuid, t2)).collect(Collectors.toList())));

    CompositeFuture.all(insertions).onComplete(tc.succeeding(h -> {
      retryQueueService.dequeueOlderThan(now).onComplete(tc.succeeding(deq1 -> {
        assertEquals(deq1, uuids1);
        retryQueueService.dequeueOlderThan(t2.plusSeconds(1)).onComplete(tc.succeeding(deq2 -> {
          assertEquals(deq2, uuids2);
          retryQueueService.dequeueOlderThan(OffsetDateTime.MAX).onComplete(tc.succeeding(deq3 -> {
            assertTrue(deq3.isEmpty());
            tc.completeNow();
          }));
        }));
      }));
    }));
  }

  @Test
  public void test_security_key_not_found(VertxTestContext tc) {
    SecurityKeyRepository securityKeyService = new SecurityKeyRepository(conn);
    securityKeyService.getBySecurityKey(UUID.randomUUID()).onComplete(tc.failingThenComplete());
  }

  @Test
  public void test_security_create(VertxTestContext tc) {
    SecurityKeyRepository securityKeyService = new SecurityKeyRepository(conn);
    securityKeyService.create(SecurityKeyScope.TENANT_MASTER, rootSecurityKeyId, tenantId).onComplete(tc.succeeding(masterKey -> {
      assertEquals(masterKey.scope(), SecurityKeyScope.TENANT_MASTER);
      assertEquals(masterKey.tenantId(), tenantId);

      securityKeyService.create(SecurityKeyScope.TENANT_USER, masterKey.id(), tenantId).onComplete(tc.succeeding(userKey -> {
        assertEquals(userKey.scope(), SecurityKeyScope.TENANT_USER);
        assertEquals(userKey.tenantId(), tenantId);
        tc.completeNow();
      }));
    }));
  }

  @Test
  public void test_security_one_root(VertxTestContext tc) {
    SecurityKeyRepository securityKeyService = new SecurityKeyRepository(conn);
    securityKeyService.create(SecurityKeyScope.ROOT, rootSecurityKeyId, tenantId).onComplete(tc.failingThenComplete());
  }

  @Test
  public void test_subscription_service(VertxTestContext tc) {

    SubscriptionRepository subscriptionService = new SubscriptionRepository(conn);
    Function<Subscription, Void> checker = sub -> {
      assertEquals(sub.consumerId(), consumerId);
      assertEquals(sub.tenantId(), tenantId);
      assertEquals(sub.topicId(), topicId);
      assertEquals(sub.url(), SUBSCRIPTION_URL);
      assertTrue(sub.isActive());
      assertFalse(sub.isFailing());
      return null;
    };

    // create
    subscriptionService.get(subscriptionId).onComplete(tc.succeeding(sub -> {
      checker.apply(sub);

      // list
      subscriptionService.list(sub.tenantId()).onComplete(tc.succeeding(subs -> {
        assertEquals(1, subs.size());
        Subscription sub0 = subs.get(0);
        checker.apply(sub0);

        // resolve
        subscriptionService.resolve(tenantId, consumerId, topicId).onComplete(tc.succeeding(resolvedSub -> {
          checker.apply(resolvedSub);

          // updates
          String newUrl = "http://newurl.com";
          subscriptionService
            .updateUrl(tenantId, sub.id(), newUrl)
            .flatMap(subscriptionService::get).onComplete(tc.succeeding(updatedUrlSub -> {
              assertEquals(updatedUrlSub.url(), newUrl);
              assertTrue(updatedUrlSub.isActive());
              assertFalse(updatedUrlSub.isFailing());
            }))
            .flatMap(sid -> subscriptionService.updateState(tenantId, sub.id(), true, false))
            .flatMap(subscriptionService::get).onComplete(tc.succeeding(updatedStateSub -> {
              assertEquals(updatedStateSub.url(), newUrl);
              assertTrue(updatedStateSub.isActive());
            })).flatMap(_void -> subscriptionService.disableAndFail(List.of(subscriptionId)).map(subscriptionId)).flatMap(subscriptionService::get).onComplete(tc.succeeding(disabledSub -> {
              assertFalse(disabledSub.isActive());
              assertTrue(disabledSub.isFailing());
              tc.completeNow();
            }));
        }));
      }));
    }));
  }

  @Test
  public void test_topic(VertxTestContext tc) {
    TopicRepository topicService = new TopicRepository(conn);
    Checkpoint cp = tc.checkpoint(3);

    topicService.findByName(tenantId, topicName).onComplete(tc.succeeding(t -> {
      Assertions.assertEquals(t.name(), topicName);
      cp.flag();
    }));

    topicService.get(topicId).onComplete(tc.succeeding(t -> {
      Assertions.assertEquals(t.name(), topicName);
      cp.flag();
    }));

    topicService.list(tenantId).onComplete(tc.succeeding(t -> {
      assertEquals(t.size(), 1);
      Assertions.assertEquals(t.get(0).name(), topicName);
      cp.flag();
    }));
  }

  @Test
  public void test_version(VertxTestContext tc) {
    VersionRepository versionService = new VersionRepository(conn);
    versionService.list(tenantId).onComplete(tc.succeeding(versions -> {
      assertEquals(versions.size(), 1);
      assertEquals(versions.get(0).version(), versionStr);
      assertEquals(versions.get(0).tenantId(), tenantId);

      versionService.asMap(tenantId).onComplete(tc.succeeding(vs -> {
        assertEquals(vs, Map.of(versionId, versionStr));
        tc.completeNow();
      }));
    }));
  }

}
