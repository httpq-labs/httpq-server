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


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.httpq.lib.JacksonModule;
import org.httpq.model.EventBusAddress;
import org.httpq.model.WebhookHttpRequest;
import org.httpq.model.WebhookHttpResponse;
import org.httpq.server.HttpApiFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;
import java.util.List;
import java.util.UUID;

@ExtendWith(VertxExtension.class)
public class WebhookHttpRequestManagerTest {

  static {
    JacksonModule.init();
  }

  // for jackson date time
  static {
    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules();
  }

  @Test
  public void test_requests(Vertx vertx, VertxTestContext tc) throws Exception {
    final List<UUID> uuids = List.of(UUID.randomUUID(), UUID.randomUUID());
    String url = "http://url.com";
    final List<WebhookHttpRequest> requests = List.of(
      new WebhookHttpRequest(uuids.get(0), new URL(url), "body1"),
      new WebhookHttpRequest(uuids.get(1), new URL(url), "body2")
    );

    EventBus eb = vertx.eventBus();

    MessageConsumer<JsonObject> consumer = eb.localConsumer(EventBusAddress.HTTP_CLIENT);
    consumer.handler(message -> {
      WebhookHttpResponse response = HttpApiFixtures.webhookHttpResponse(UUID.fromString(message.body().getString("eventId")));
      message.reply(JsonObject.mapFrom(response));
    });

    consumer.completionHandler(tc.succeeding(ready -> {
      WebhookHttpRequestManager webhookHttpRequestManager = new WebhookHttpRequestManager(eb);
      webhookHttpRequestManager.apply(requests).onComplete(tc.succeedingThenComplete());
    }));
  }
}
