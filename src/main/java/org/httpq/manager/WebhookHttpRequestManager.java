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
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.httpq.model.EventBusAddress;
import org.httpq.model.WebhookHttpRequest;
import org.httpq.model.WebhookHttpResponse;

import java.util.Collection;

public class WebhookHttpRequestManager {
  private final EventBus eb;

  public WebhookHttpRequestManager(EventBus eb) {
    this.eb = eb;
  }

  public Future<Collection<WebhookHttpResponse>> apply(Collection<WebhookHttpRequest> reqs) {

    // make http request
    // update and return
    // decide if we retry or not if it's retriable (not successful)
    // if retry: add to retry queue for later
    // if past max retry attempts, disable subscription
    // notify tenant via admin endpoint
    return CompositeFuture.join(reqs.stream()
        .map(this::actuateHttpRequest).toList()).map(CompositeFuture::list);
  }

  private Future actuateHttpRequest(WebhookHttpRequest req) {
    return eb.<JsonObject>request(EventBusAddress.HTTP_CLIENT, JsonObject.mapFrom(req))
      .map(h -> h.body().mapTo(WebhookHttpResponse.class));
  }
}
