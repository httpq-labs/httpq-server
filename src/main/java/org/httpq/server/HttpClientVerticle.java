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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.httpq.model.EventBusAddress;
import org.httpq.model.WebhookHttpRequest;
import org.httpq.model.WebhookHttpResponse;
import org.httpq.model.WebhookState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

public class HttpClientVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(HttpClientVerticle.class);
  public final static Duration REQ_TIMEOUT = Duration.ofMillis(2950);
  WebClient webClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.info("starting");
    webClient = WebClient.create(vertx, new WebClientOptions()
      .setUserAgent("httpq/1.0.0"));

    MessageConsumer<JsonObject> consumer =
      vertx.eventBus().localConsumer(EventBusAddress.HTTP_CLIENT);

    consumer.handler(h -> {
      WebhookHttpRequest req = h.body().mapTo(WebhookHttpRequest.class);
      query(req).onSuccess(rep -> {
        h.reply(JsonObject.mapFrom(rep));
      });
    });

    consumer.completionHandler(res -> startPromise.complete());

  }

  public Future<WebhookHttpResponse> query(WebhookHttpRequest req) {
    Instant start = Instant.now();
    return
      webClient
        .post(req.port(), req.host(), req.uri())
        .ssl(req.tls())
        .timeout(REQ_TIMEOUT.toMillis())
        .send()
        .map(resp -> switch (resp.statusCode()) {
          case 200 -> new WebhookHttpResponse(
            req.eventId(),
            WebhookState.S600,
            resp.statusCode(),
            resp.bodyAsString(),
            Duration.between(start, Instant.now()),
            req.url().toString());
          case 429 -> new WebhookHttpResponse(
            req.eventId(),
            WebhookState.S705,
            resp.statusCode(),
            null,
            Duration.between(start, Instant.now()),
            req.url().toString());
          default -> new WebhookHttpResponse(
            req.eventId(),
            WebhookState.S700,
            resp.statusCode(),
            resp.bodyAsString(),
            Duration.between(start, Instant.now()),
            req.url().toString());
        }).onFailure(t -> {
          LOGGER.warn("failed to deliver webhook", t);
        })
        .otherwise(t -> {
          WebhookState state = WebhookState.S799;
          if (t instanceof UnknownHostException) {
            state = WebhookState.S701;
          } else if (t instanceof HttpClosedException) {
            state = WebhookState.S703;
          } else if (t instanceof TimeoutException) {
            state = WebhookState.S703;
          } else if (t instanceof SSLException) {
            state = WebhookState.S704;
          }
          return new WebhookHttpResponse(
          req.eventId(),
          state,
          0,
          null,
            Duration.between(start, Instant.now()),
            req.url().toString());
        });

  }
}
