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
package org.httpq.server;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.httpq.lib.JacksonModule;
import org.httpq.model.EventBusAddress;
import org.httpq.model.WebhookHttpRequest;
import org.httpq.model.WebhookHttpResponse;
import org.httpq.model.WebhookState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class HttpClientTests {

  static String testEndpoint = HttpApiFixtures.testEndpoint;

  static JsonObject payload = new JsonObject().put("hello", "world");
  static EventBus eb;

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext tc) {
    JacksonModule.init();
    vertx.deployVerticle(new HttpClientVerticle(), tc.succeeding(res -> tc.completeNow()));
    eb = vertx.eventBus();
  }

  @Test
  public void test200(VertxTestContext tc) {
    UUID id = UUID.randomUUID();
    WebhookHttpRequest req = WebhookHttpRequest.make(id,testEndpoint+"/post", payload.encode());
    eb.<JsonObject>request(EventBusAddress.HTTP_CLIENT, JsonObject.mapFrom(req)).onComplete(tc.succeeding(h -> {
      WebhookHttpResponse rep = fromJson(h.body());
      assertEquals(200, rep.statusCode());
      assertEquals(id, rep.eventId());
      Assertions.assertEquals(WebhookState.S600, rep.state());
      tc.completeNow();
    }));

  }

  @Test
  public void test429(VertxTestContext tc) {

    WebhookHttpRequest req = WebhookHttpRequest.make(UUID.randomUUID(), testEndpoint+"/status/429", payload.encode());
    eb.<JsonObject>request(EventBusAddress.HTTP_CLIENT, JsonObject.mapFrom(req)).onComplete(tc.succeeding(h -> {
      WebhookHttpResponse rep = fromJson(h.body());
      assertEquals(429, rep.statusCode());
      assertEquals(WebhookState.S705, rep.state());
      tc.completeNow();
    }));
  }

  @Test
  public void testOther(VertxTestContext tc) {
    WebhookHttpRequest req = WebhookHttpRequest.make(UUID.randomUUID(), testEndpoint+"/status/500", payload.encode());
    eb.<JsonObject>request(EventBusAddress.HTTP_CLIENT, JsonObject.mapFrom(req)).onComplete(tc.succeeding(h -> {
      WebhookHttpResponse rep = fromJson(h.body());
      assertEquals(500, rep.statusCode());
      assertEquals(WebhookState.S700, rep.state());
      tc.completeNow();
    }));
  }

  @Test
  public void testUnknownHost(VertxTestContext tc) {
    WebhookHttpRequest req = WebhookHttpRequest.make(UUID.randomUUID(),"http://blah", payload.encode());
    eb.<JsonObject>request(EventBusAddress.HTTP_CLIENT, JsonObject.mapFrom(req)).onComplete(tc.succeeding(h -> {
      WebhookHttpResponse rep = fromJson(h.body());
      assertEquals(0, rep.statusCode());
      assertEquals(WebhookState.S701, rep.state());
      tc.completeNow();
    }));
  }

  @Test
  public void test3secondsTimeout(VertxTestContext tc) {
    long delay = HttpClientVerticle.REQ_TIMEOUT.toMillis() * 100;
    WebhookHttpRequest req = WebhookHttpRequest.make(UUID.randomUUID(), testEndpoint+"/delay/"+delay, payload.encode());
    final Instant start = Instant.now();
    eb.<JsonObject>request(EventBusAddress.HTTP_CLIENT, JsonObject.mapFrom(req)).onComplete(tc.succeeding(h -> {
      Instant end = Instant.now();
      assertTrue(Duration.between(start, end).toSeconds() < 4);
      WebhookHttpResponse rep = fromJson(h.body());
      assertEquals(WebhookState.S703, rep.state());
      assertEquals(0, rep.statusCode());
      tc.completeNow();
    }));
  }

  @AfterAll
  public static void teardown(Vertx vertx) {
    vertx.close();
  }

  public WebhookHttpResponse fromJson(JsonObject o) {
    return o.mapTo(WebhookHttpResponse.class);
  }
}
