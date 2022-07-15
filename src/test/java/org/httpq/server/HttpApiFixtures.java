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

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import org.httpq.model.TypedConf;
import org.httpq.model.WebhookHttpRequest;
import org.httpq.model.WebhookHttpResponse;
import org.httpq.model.WebhookState;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


public class HttpApiFixtures {
  static TypedConf typedConf;

  public HttpApiFixtures(TypedConf typedConf) {
    HttpApiFixtures.typedConf = typedConf;
  }

  static String X_API_KEY = "X-Api-Key";

  static String testEndpoint = System.getenv().getOrDefault("HTTPBIN_URL", "http://0.0.0.0:8777");

  public RequestSpecification reqSpec(UUID apiKey) {
    return new RequestSpecBuilder()
      .addHeader(X_API_KEY, apiKey.toString())
      .setBaseUri("http://%s".formatted(typedConf.httpHost()))
      .setPort(typedConf.httpPort())
      .setContentType(ContentType.JSON)
      .build();
  }

  private RequestSpecification rootSpec() {
    return reqSpec(getRootApiKey());
  }

  public UUID createConsumer(RequestSpecification spec, UUID versionId, int expectedRetCode) {
    return createConsumer(spec, null, versionId, expectedRetCode);
  }

  public UUID createUserSecurityKey(RequestSpecification spec, int expectedRetCode) {
    return createTenantSecurityKey(spec, null, expectedRetCode);
  }

  public UUID createTenantSecurityKey(RequestSpecification spec, UUID tenant, int expectedRetCode) {
    JsonObject o = new JsonObject();
    if (tenant != null) {
      o.put("tenantId", tenant);
    }

    ValidatableResponse resp = createRawEntity(spec, "/keys", o, expectedRetCode);
    if (expectedRetCode / 100 == 2) {
      return UUID.fromString(resp.extract().path("securityKey"));
    }
    return null;
  }

  private ValidatableResponse createRawEntity(RequestSpecification spec, String endpoint, JsonObject o, int expectedRetCode) {

    return RestAssured.given(spec)
      .log().body()
      .body(o.encode()).post(endpoint)
      .then()
      .log().body()
      .statusCode(expectedRetCode);

  }

  private ValidatableResponse updateRawEntity(RequestSpecification spec, String endpoint, JsonObject o, int expectedRetCode) {

    return RestAssured.given(spec)
      .log().body()
      .body(o.encode()).put(endpoint)
      .then()
      .log().body()
      .statusCode(expectedRetCode);

  }


  private UUID createEntity(RequestSpecification spec, String endpoint, JsonObject o, int expectedRetCode) {

    ValidatableResponse resp = createRawEntity(spec, endpoint, o, expectedRetCode);

    if (expectedRetCode / 100 == 2) {
      return UUID.fromString(resp.extract().path("id"));
    }

    return null;
  }

  private void updateEntity(RequestSpecification spec, String endpoint, JsonObject o, int expectedRetCode) {
    updateRawEntity(spec, endpoint, o, expectedRetCode);
  }

  public UUID createSubscription(RequestSpecification userSpec, UUID topicId, UUID consumerId, String url, int retCode) {
    JsonObject o = new JsonObject();

    o.put("consumerId", consumerId);
    o.put("topicId", topicId);
    o.put("url", url);

    return createEntity(userSpec, "/subscriptions", o, retCode);
  }

  public void updateSubscription(RequestSpecification userSpec, UUID id, String url) {
    JsonObject o = new JsonObject();

    o.put("url", url);

    updateEntity(userSpec, "/subscriptions/"+id.toString(), o, 200);
  }

  public List<String> getIds(RequestSpecification spec, String endpoint) {
    return RestAssured.given(spec)
      .get(endpoint)
      .then()
      //.log().body()
      .statusCode(200)
      .extract().jsonPath().getList("items.id");
  }

  public JsonObject getEntity(RequestSpecification spec, String endpoint, UUID id) {
    return new JsonObject(RestAssured.given(spec)
      .get(endpoint + "/" + id.toString())
      .then()
      .log().body()
      .statusCode(200)
      .extract().asString());
  }

  public UUID createTopic(RequestSpecification masterSpec, String name, int expectedRetCode) {
    JsonObject o = new JsonObject();

    o.put("name", name);
    return createEntity(masterSpec, "/topics", o, expectedRetCode);
  }

  public UUID createVersion(RequestSpecification spec, String version, int expectedRetCode) {
    JsonObject o = new JsonObject();

    o.put("version", version);
    return createEntity(spec, "/versions", o, expectedRetCode);

  }

  public UUID createConsumer(RequestSpecification spec, String externalId, UUID versionId, int expectedRetCode) {
    JsonObject o = new JsonObject();
    if (externalId != null) {
      o.put("externalId", externalId);
    }
    o.put("versionId", versionId);

    return createEntity(spec, "/consumers", o, expectedRetCode);
  }

  public UUID createWebhook(RequestSpecification spec,
                            UUID consumer,
                            String topic,
                            JsonObject versionedPayloads,
                            int expectedRetCode) {

    JsonObject o = new JsonObject();
    o.put("consumerId", consumer);
    o.put("topic", topic);
    o.put("versionedPayloads", versionedPayloads);


    return createEntity(spec, "/events", o, expectedRetCode);
  }

  public UUID createTenant(RequestSpecification spec, String tenantName, int retCode) {
    JsonObject o = new JsonObject();
    o.put("name", tenantName);

    return createEntity(spec, "/tenants", o, retCode);
  }

  AtomicInteger versionCounter = new AtomicInteger();

  public TestData setupTestData() {
    return setupTestData("/post");
  }
  public TestData setupTestData(String endpointPath) {
    RequestSpecification rootSpec = rootSpec();
    UUID tenant = createTenant(rootSpec, UUID.randomUUID().toString(), 201);
    UUID masterKey = createTenantSecurityKey(rootSpec(), tenant, 201);
    RequestSpecification masterSpec = reqSpec(masterKey);
    UUID userKey = createUserSecurityKey(masterSpec, 201);
    RequestSpecification userSpec = reqSpec(userKey);
    String version1Str = "v%s".formatted(versionCounter.incrementAndGet());
    String version2Str ="v%s".formatted(versionCounter.incrementAndGet());
    UUID version1 = createVersion(masterSpec, version1Str, 201);
    UUID version2 = createVersion(masterSpec, version2Str, 201);
    UUID consumer = createConsumer(userSpec, version1, 201);
    String topicStr = UUID.randomUUID().toString().substring(16);
    UUID topic = createTopic(masterSpec, topicStr, 201);
    UUID subscription = createSubscription(userSpec, topic, consumer, testEndpoint + endpointPath, 201);
    return new TestData(
      tenant, consumer, topic, topicStr, subscription, version1, version1Str, version2, version2Str, rootSpec, masterSpec, userSpec);
  }


  // helpers for good measure
  Connection conn() {
    try {
      return DriverManager.getConnection(typedConf.jdbcUrl());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  ResultSet selectOne(String q) {
    try (Connection conn = conn()) {
      final Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(q);
      rs.next();
      return rs;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }



  public UUID getRootApiKey() {
    try {
      return UUID.fromString(selectOne("SELECT security_key FROM security_keys WHERE scope = 'ROOT'").getString("security_key"));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static WebhookHttpRequest webhookHttpRequest(UUID eventId) {
    return WebhookHttpRequest.make(eventId, "http://url.com", "body");
  }

  public static WebhookHttpResponse webhookHttpResponse(UUID eventId) {
    return new WebhookHttpResponse(eventId, WebhookState.S600, 200, "body", Duration.ofMillis(345), "http://url.com");
  }
}
