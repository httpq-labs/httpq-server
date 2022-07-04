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
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.httpq.model.WebhookHttpRequest;
import org.httpq.model.WebhookHttpResponse;
import org.httpq.server.HttpApiFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class WebhookSendPipelineTest {
  @Mock
  WebhookHttpRequestManager requestManager;
  @Mock
  WebhookHttpResponseManager responseManager;
  @Mock
  WebhookPreparerManager preparerManager;

  @Test
  public void test_manager(VertxTestContext tc) {

    List<UUID> eventIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    List<WebhookHttpRequest> requests = List.of(HttpApiFixtures.webhookHttpRequest(UUID.randomUUID()), HttpApiFixtures.webhookHttpRequest(UUID.randomUUID()));
    List<WebhookHttpResponse> responses = List.of(HttpApiFixtures.webhookHttpResponse(UUID.randomUUID()), HttpApiFixtures.webhookHttpResponse(UUID.randomUUID()));
    WebhookSendPipeline webhookSendPipeline = new WebhookSendPipeline(preparerManager, requestManager, responseManager);

    when(preparerManager.apply(eq(eventIds))).thenReturn(Future.succeededFuture(requests));
    when(requestManager.apply(eq(requests))).thenReturn(Future.succeededFuture(responses));
    when(responseManager.apply(eq(responses))).thenReturn(Future.succeededFuture());
    webhookSendPipeline.apply(eventIds).onComplete(tc.succeedingThenComplete());
  }
}
