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

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AttemptedWebhookTest {
  public AttemptedWebhook w(int tryCount, WebhookState state, OffsetDateTime now) {
    return new AttemptedWebhook(UUID.randomUUID(), state, UUID.randomUUID(), tryCount, now);
  }

  @Test
  public void test_next_retry() {
    // 3^n  {3, 9, 27, 81, 243, 729, 2187, 6561, 19683, 59049}

    final OffsetDateTime now = OffsetDateTime.now();
    assertThrows(IllegalStateException.class, () -> {
      w(0, WebhookState.S600, now).nextAttempt();
    });
    assertThrows(IllegalStateException.class, () -> {
      w(1, WebhookState.S600, now).nextAttempt();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      w(0, WebhookState.S700, now).nextAttempt();
    });
    assertEquals(w(1, WebhookState.S700, now).nextAttempt(), now.plusSeconds(3));
    assertEquals(w(2, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(9));
    assertEquals(w(3, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(27));
    assertEquals(w(4, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(81));     // 1m21s
    assertEquals(w(5, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(243));    // 4m30s
    assertEquals(w(6, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(729));    // 12m09s
    assertEquals(w(7, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(2187));   // 36m27s
    assertEquals(w(8, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(6561));   // 1h49m21s
    assertEquals(w(9, WebhookState.S700, now).nextAttempt(),  now.plusSeconds(19683));  // 5h28m03s
    assertThrows(IllegalStateException.class, () -> {
      w(10, WebhookState.S700, now).nextAttempt(); // 16h24m9s
    });
  }
}
