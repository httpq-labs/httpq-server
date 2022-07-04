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
import org.httpq.model.AttemptedWebhook;
import org.httpq.model.WebhookHttpResponse;
import org.httpq.model.WebhookState;
import org.httpq.repository.RetryQueueRepository;
import org.httpq.repository.SubscriptionRepository;
import org.httpq.repository.WebhookEventRepository;

import java.util.Collection;
import java.util.List;

public class WebhookHttpResponseManager {

  private final RetryQueueRepository retryQueueRepository;
  private final WebhookEventRepository webhookEventRepository;
  private final SubscriptionRepository subscriptionRepository;

  // disable subscriptions when all retries are exhausted
  public WebhookHttpResponseManager(
    RetryQueueRepository retryQueueRepository,
    WebhookEventRepository webhookEventRepository,
    SubscriptionRepository subscriptionRepository) {
    this.retryQueueRepository = retryQueueRepository;
    this.webhookEventRepository = webhookEventRepository;
    this.subscriptionRepository = subscriptionRepository;
  }


  public Future<Void> apply(Collection<WebhookHttpResponse> responses) {

    return webhookEventRepository.recordWebhookAttempt(responses).flatMap(updatedWebhookEvents -> {
      // discard successes. we're done!
      List<AttemptedWebhook> failedWebhooks = updatedWebhookEvents
        .stream()
        .filter(w -> w.state() != WebhookState.S600).toList();

      // retry what's retriable
      List<AttemptedWebhook> failedRemainingTries = failedWebhooks.stream().filter(AttemptedWebhook::isRetriable).toList();

      // fail subscriptions when retries are exhausted
      List<AttemptedWebhook> failedExhaustedTries = failedWebhooks.stream().filter(w -> !w.isRetriable()).toList();

      return CompositeFuture.all(
        retryQueueRepository.enqueue(failedRemainingTries.stream().map(AttemptedWebhook::scheduleNextAttempt).toList()),
        subscriptionRepository.disableAndFail(failedExhaustedTries.stream().map(AttemptedWebhook::eventId).toList())
      );
    }).mapEmpty();
  }

}
