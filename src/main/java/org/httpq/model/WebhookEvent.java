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

import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WebhookEvent(
  UUID id,
  WebhookState state,
  UUID tenantId,
  UUID consumerId,
  UUID topicId,
  UUID versionId,
  UUID subscriptionId,
  Integer tryCount,

  Headers requestHeaders,
  JsonObject requestBody,

  String url,
  Headers responseHeaders,
  String responseBody,
  Integer responseCode,
  Duration duration,

  OffsetDateTime createdAt,
  OffsetDateTime updatedAt,
  OffsetDateTime deliveredAt
) {

}
