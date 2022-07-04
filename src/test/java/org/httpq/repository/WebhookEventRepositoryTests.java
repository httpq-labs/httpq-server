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

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.httpq.model.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebhookEventRepositoryTests extends TestWithRepositoryFixtures {

  static String tableName = "webhook_events";

  // we fix this for testing, else it'll use dynamic localdate from fixtures
  LocalDate today = LocalDate.of(2020, 1, 1);


  @Test
  @Order(1)
  public void test_partitions(VertxTestContext tc) {
    DailyPartitionTableRepository repo = new DailyPartitionTableRepository(conn, tableName, today);
    final Set<DailyPartitionTable> desiredState = repo.computeDesiredPartitionsState();

    Set<DailyPartitionTable> expected = today.minusDays(7).datesUntil(today.plusDays(7)).map(date -> new DailyPartitionTable(tableName, date)).collect(Collectors.toSet());

    assertEquals(expected, desiredState);

    repo.listCurrentPartitions().onComplete(tc.succeeding(partitions -> {
      assertTrue(partitions.isEmpty());
      repo.reconcile(desiredState).onComplete(tc.succeeding(reconciliationResult -> {
        Assertions.assertEquals(
          new StateReconciliationSet<>(
            Set.of(
              DailyPartitionTable.parse("webhook_events__2019_12_25"),
              DailyPartitionTable.parse("webhook_events__2019_12_26"),
              DailyPartitionTable.parse("webhook_events__2019_12_27"),
              DailyPartitionTable.parse("webhook_events__2019_12_28"),
              DailyPartitionTable.parse("webhook_events__2019_12_29"),
              DailyPartitionTable.parse("webhook_events__2019_12_30"),
              DailyPartitionTable.parse("webhook_events__2019_12_31"),
              DailyPartitionTable.parse("webhook_events__2020_01_01"),
              DailyPartitionTable.parse("webhook_events__2020_01_02"),
              DailyPartitionTable.parse("webhook_events__2020_01_03"),
              DailyPartitionTable.parse("webhook_events__2020_01_04"),
              DailyPartitionTable.parse("webhook_events__2020_01_05"),
              DailyPartitionTable.parse("webhook_events__2020_01_06"),
              DailyPartitionTable.parse("webhook_events__2020_01_07")
              ),
            Set.of()),
          reconciliationResult
        );
        tc.completeNow();
      }));
    }));
  }


  @Test
  @Order(2)
  public void test_webhook_event(VertxTestContext tc)  {

    WebhookEventRepository wes = new WebhookEventRepository(conn, today);
    JsonObject payload = new JsonObject().put("hello", "world");
    wes.create(tenantId, consumerId, topicId, versionId, subscriptionId, payload).flatMap(uuid -> wes.get(tenantId, uuid)).onComplete(tc.succeeding(w -> {
      Assertions.assertEquals(w.consumerId(), consumerId);
      Assertions.assertEquals(w.state(), WebhookState.S000);
      Assertions.assertEquals(w.versionId(), versionId);
      Assertions.assertEquals(w.requestBody(), payload);
      assertNull(w.url());
      Assertions.assertEquals(w.subscriptionId(), subscriptionId);
      Assertions.assertEquals(w.topicId(), topicId);
      Assertions.assertEquals(w.tryCount(), 0);
      assertNull(w.responseBody());
      assertNull(w.responseCode());

      wes.listForSending(List.of(w.id())).onComplete(tc.succeeding(forSending -> {
        assertEquals(forSending.size(), 1);
        WebhookHttpRequest req = forSending.get(0);
        Assertions.assertEquals(req.eventId(), w.id());
        assertEquals(req.url().toString(),SUBSCRIPTION_URL);
        assertEquals(req.body(), payload.encode());

        String responseBody = "i am a teapot";
        String urlQueried = "http://url.com";
        List<WebhookHttpResponse> updates = List.of(
          new WebhookHttpResponse(w.id(), WebhookState.S600, 201, responseBody, Duration.ofMillis(324), urlQueried)
        );

        wes
          .recordWebhookAttempt(updates)
          .map(updated -> {
            assertEquals(updated.size(), 1);
            AttemptedWebhook updatedWebhook = updated.get(0);

            assertEquals(updatedWebhook.eventId(), updates.get(0).eventId());
            assertEquals(updatedWebhook.subscriptionId(), subscriptionId);

            return updated;
          })
          .flatMap(_void -> wes.list(List.of(w.id()))).onComplete(tc.succeeding(updated -> {
            assertEquals(updated.size(), 1);
            WebhookEvent updatedWebhook = updated.get(0);

            assertEquals(updatedWebhook.consumerId(), consumerId);
            assertEquals(updatedWebhook.state(), WebhookState.S600);
            assertEquals(updatedWebhook.versionId(), versionId);
            assertEquals(updatedWebhook.requestBody(), payload);
            assertEquals(updatedWebhook.url(), urlQueried);
            assertEquals(updatedWebhook.subscriptionId(), subscriptionId);
            assertEquals(updatedWebhook.topicId(), topicId);
            assertEquals(updatedWebhook.tryCount(), 1);
            assertEquals(updatedWebhook.responseBody(), responseBody);
            assertEquals(updatedWebhook.responseCode(), 201);

            tc.completeNow();
          }));
      }));


    }));
  }

  @Test
  @Order(3)
  public void test_update_partitions(VertxTestContext tc) {
    today = today.plusDays(2);
    final DailyPartitionTableRepository repo2 = new DailyPartitionTableRepository(conn, tableName, today);

    final Set<DailyPartitionTable> newDesiredState = repo2.computeDesiredPartitionsState();

    repo2.reconcile(newDesiredState).onComplete(tc.succeeding(reconciliationResult -> {
      Set<DailyPartitionTable> expectedToAdd = Set.of(
        DailyPartitionTable.parse("webhook_events__2020_01_09"),
        DailyPartitionTable.parse("webhook_events__2020_01_08")
      );
      Set<DailyPartitionTable> expectedToDrop = Set.of(
        DailyPartitionTable.parse("webhook_events__2019_12_25"),
        DailyPartitionTable.parse("webhook_events__2019_12_26")
      );
      assertEquals(new StateReconciliationSet<>(expectedToAdd, expectedToDrop), reconciliationResult);
      tc.completeNow();
    }));
  }
}
