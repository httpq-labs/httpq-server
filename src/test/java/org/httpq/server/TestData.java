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

import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

public record TestData(
  UUID tenant,
  UUID consumer,
  UUID topic,
  String topicStr,
  UUID subscription,
  UUID version1,
  String version1Str,
  UUID version2,
  String version2Str,
  RequestSpecification rootSpec,
  RequestSpecification masterSpec,
  RequestSpecification userSpec) {
  public JsonObject versionedPayload() {
    JsonObject versionedPayload = new JsonObject();
    versionedPayload.put(version1Str, new JsonObject().put("msg", "hello"));
    versionedPayload.put(version2Str, new JsonObject().put("message", "hello"));
    return versionedPayload;
  }
}
