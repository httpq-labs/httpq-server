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
import org.httpq.model.WebhookHttpRequest;
import org.httpq.repository.WebhookEventRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class WebhookPreparerManager {
  private final WebhookEventRepository webhookEventRepository;

  public WebhookPreparerManager(WebhookEventRepository webhookEventRepository) {
    this.webhookEventRepository = webhookEventRepository;
  }

  public Future<List<WebhookHttpRequest>> apply(Collection<UUID> eventIds) {
    return webhookEventRepository.listForSending(eventIds);

  }
}
