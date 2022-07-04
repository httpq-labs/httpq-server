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
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.httpq.model.AttemptedWebhook;
import org.httpq.model.WebhookHttpResponse;
import org.httpq.model.WebhookState;
import org.httpq.repository.RetryQueueRepository;
import org.httpq.repository.SubscriptionRepository;
import org.httpq.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class WebhookHttpResponseManagerTest {

  @Mock
  RetryQueueRepository retryQueueRepository;

  @Mock
  WebhookEventRepository webhookEventRepository;

  @Mock
  SubscriptionRepository subscriptionRepository;

  WebhookHttpResponseManager responseManager;

  @BeforeEach
  public void setup() {
    responseManager = new WebhookHttpResponseManager(retryQueueRepository, webhookEventRepository, subscriptionRepository);
  }
  @Test
  public void test_responses(VertxTestContext tc) {
    UUID eventDelivered = UUID.randomUUID();
    UUID eventNotDelivered = UUID.randomUUID();
    UUID eventBackoff = UUID.randomUUID();
    List<WebhookHttpResponse> responses = List.of(
      new WebhookHttpResponse(eventDelivered, WebhookState.S600, 200, "body", Duration.ofMillis(435), "url"),
      new WebhookHttpResponse(eventNotDelivered, WebhookState.S700, 500, "body", Duration.ofMillis(43), "url2"),
      new WebhookHttpResponse(eventBackoff, WebhookState.S705, 429, null, Duration.ofMillis(75), "url3")
    );

    OffsetDateTime now = OffsetDateTime.now();
    AttemptedWebhook wRetry = new AttemptedWebhook(eventNotDelivered, WebhookState.S700, UUID.randomUUID(), 2, now);
    when(webhookEventRepository.recordWebhookAttempt(responses)).thenReturn(Future.succeededFuture(List.of(
      new AttemptedWebhook(eventDelivered, WebhookState.S600, UUID.randomUUID(), 1, now),
      wRetry,
      new AttemptedWebhook(eventBackoff, WebhookState.S705, UUID.randomUUID(), 10, now )
    )));
    when(retryQueueRepository.enqueue(List.of(wRetry.scheduleNextAttempt()))).thenReturn(Future.succeededFuture());
    when(subscriptionRepository.disableAndFail(List.of(eventBackoff))).thenReturn(Future.succeededFuture());
    responseManager.apply(responses).onComplete(tc.succeedingThenComplete());
  }


}
