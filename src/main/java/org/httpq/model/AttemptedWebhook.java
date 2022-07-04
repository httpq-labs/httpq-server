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
package org.httpq.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttemptedWebhook(UUID eventId, WebhookState state, UUID subscriptionId, Integer tryCount, OffsetDateTime createdAt){
  // 10 failures
  // 3^n  {3, 9, 27, 81, 243, 729, 2187, 6561, 19683, 59049}
  // 2^n {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024}
  private final static int BASE = 3;

  public OffsetDateTime nextAttempt() {
    if (!isRetriable()) {
      throw new IllegalStateException("cannot schedule a non retriable webhook");
    }
    if (tryCount < 1) {
      throw new IllegalArgumentException("tryCount must be >= 1");
    }
    return createdAt.plusSeconds((int)Math.pow(BASE, tryCount));
  }

  public ScheduledWebhook scheduleNextAttempt() {
    return new ScheduledWebhook(eventId, nextAttempt());
  }

  private final static int MAX_TRIES = 10;
  public boolean isRetriable() {
    return state != WebhookState.S600 && tryCount < MAX_TRIES;
  }

}
