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

import java.util.Collection;
import java.util.UUID;

public class WebhookSendPipeline {

  WebhookPreparerManager preparerManager;
  WebhookHttpRequestManager requestManager;
  WebhookHttpResponseManager responseManager;

  public WebhookSendPipeline(WebhookPreparerManager preparerManager,
                             WebhookHttpRequestManager requestManager,
                             WebhookHttpResponseManager responseManager) {
    this.preparerManager = preparerManager;
    this.requestManager = requestManager;
    this.responseManager = responseManager;
  }

  public Future<Void> apply(Collection<UUID> ids) {
    return preparerManager.apply(ids).flatMap(requestManager::apply).flatMap(responseManager::apply);
  }
}
