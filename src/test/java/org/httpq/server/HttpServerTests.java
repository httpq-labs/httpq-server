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
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.flywaydb.core.Flyway;
import org.httpq.db.Database;
import org.httpq.db.FlywayFactory;
import org.httpq.model.TypedConf;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.*;

@ExtendWith(VertxExtension.class)
public class HttpServerTests {

  static TypedConf typedConf;
  static HttpApiFixtures httpApiFixtures;
  static PgPool pool;

  static Flyway flyway;
  static UUID tenant;
  static UUID version1;
  static String version1Str;
  static UUID version2;
  static String version2Str;
  static RequestSpecification rootSpec;
  static RequestSpecification aliceMasterSpec;
  static RequestSpecification aliceUserSpec;
  static EventBus eb;
  static TestData testData;
  @BeforeAll
  static void beforeAll(Vertx vertx, VertxTestContext testContext) {
    typedConf = TypedConf.load();
    flyway = FlywayFactory.make(typedConf);
    flyway.clean();
    flyway.migrate();

    pool = new Database(vertx).getSharedPool();
    httpApiFixtures = new HttpApiFixtures(typedConf);
    eb = vertx.eventBus();

    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> {

      testData = httpApiFixtures.setupTestData();
      rootSpec = testData.rootSpec();
      tenant = testData.tenant();
      aliceMasterSpec = testData.masterSpec();
      aliceUserSpec = testData.userSpec();
      version1 = testData.version1();
      version1Str = testData.version1Str();
      version2 = testData.version2();
      version2Str = testData.version2Str();
      testContext.completeNow();
    }));
  }



  @BeforeEach
  public void beforeEach() {
    RestAssured.reset();
    //RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    baseURI = "http://%s".formatted(typedConf.httpHost());
    port = typedConf.httpPort();

  }

  @AfterAll
  static void afterAll(Vertx vertx) {
    pool.close().onSuccess(s -> vertx.close());
  }

  @Test
  void unauthorized() {
    requestSpecification = null;
    get("/tenants/" + UUID.randomUUID()).then().statusCode(401);
  }

  @Test
  void tenants_validation() {
    // some validation
    httpApiFixtures.createTenant(rootSpec, "", 400);

    httpApiFixtures.createTenant(rootSpec, null, 400);

  }

  @Test
  void security_keys_create_and_get() {
    // keys and stuff
    UUID aliceUserKey1 = httpApiFixtures.createUserSecurityKey(aliceMasterSpec, 201);
    UUID aliceUserKey2 = httpApiFixtures.createUserSecurityKey(aliceMasterSpec, 201);
    Assertions.assertNotEquals(aliceUserKey1, aliceUserKey2);

    UUID tenantBob = httpApiFixtures.createTenant(rootSpec, "bob", 201);

    UUID bobMasterKey = httpApiFixtures.createTenantSecurityKey(rootSpec, tenantBob, 201);
    RequestSpecification bobSpec = httpApiFixtures.reqSpec(bobMasterKey);
    UUID bobUserKey1 = httpApiFixtures.createUserSecurityKey(bobSpec, 201);
    Assertions.assertNotEquals(bobMasterKey, bobUserKey1);

    // max 1 tenant master key per tenant
    httpApiFixtures.createTenantSecurityKey(rootSpec, tenantBob, 400);

    // consumer key cannot create a master key
    httpApiFixtures.createTenantSecurityKey(httpApiFixtures.reqSpec(bobUserKey1), tenantBob, 401);

  }

  @Test
  void consumers_create_and_list() {
    // nothing at first
    //Assertions.assertTrue(fixtures.getIds(aliceUserSpec, "/consumers").isEmpty());

    // only tenant user can create consumers
    httpApiFixtures.createConsumer(aliceMasterSpec, null, version1, 401);

    // create consumer w/ external id
    UUID consumer1 = httpApiFixtures.createConsumer(aliceUserSpec, "1", version1, 201);

    // external ID is unique per tenant
    httpApiFixtures.createConsumer(aliceUserSpec, "1", version1, 400);

    // external ID is optional
    UUID consumer2 = httpApiFixtures.createConsumer(aliceUserSpec, null, version1, 201);

    // retrieve
    List<String> returnedIds = httpApiFixtures.getIds(aliceUserSpec, "/consumers");
    //Assertions.assertEquals(List.of(consumer1.toString(), consumer2.toString()), returnedIds);
  }

  @Test
  void subscriptions_create_and_list() {
    // nothing at first
    //Assertions.assertTrue(fixtures.getIds(aliceUserSpec, "/subscriptions").isEmpty());

    UUID topic1 = httpApiFixtures.createTopic(aliceMasterSpec, "subscription.create", 201);
    UUID topic2 = httpApiFixtures.createTopic(aliceMasterSpec, "subscription.delete", 201);

    UUID consumer = httpApiFixtures.createConsumer(aliceUserSpec, version2, 201);
    String url = "https://yo";

    UUID subscription1 = httpApiFixtures.createSubscription(aliceUserSpec, topic1, consumer, url, 201);
    UUID subscription2 = httpApiFixtures.createSubscription(aliceUserSpec, topic2, consumer, url, 201);

    // each only one topic per subscription
    httpApiFixtures.createSubscription(aliceUserSpec, topic2, consumer, url, 400);

    // only user key can create subscription
    httpApiFixtures.createSubscription(aliceMasterSpec, topic2, consumer, url, 401);


    // retrieve
    List<String> returnedIds = httpApiFixtures.getIds(aliceUserSpec, "/subscriptions");
    //Assertions.assertEquals(Set.of(subscription1.toString(), subscription2.toString()), new HashSet<>(returnedIds));
  }

  @Test
  void topics_create_and_list() {
    // nothing at first
    //Assertions.assertTrue(fixtures.getIds(aliceMasterSpec, "/topics").isEmpty());

    // create two topics
    UUID topic1 = httpApiFixtures.createTopic(aliceMasterSpec, "user.created", 201);
    UUID topic2 = httpApiFixtures.createTopic(aliceMasterSpec, "user.deleted", 201);

    // each topic is unique to the tenant
    httpApiFixtures.createTopic(aliceMasterSpec, "user.deleted", 400);

    // only master key can create topics
    httpApiFixtures.createTopic(aliceUserSpec, "user.new", 401);

    // retrieve versions
    List<String> returnedIds = httpApiFixtures.getIds(aliceMasterSpec, "/topics");
    //Assertions.assertEquals(Set.of(topic1.toString(), topic2.toString()), new HashSet<>(returnedIds));
  }

  @Test
  void versions_create_and_list() {
    // nothing at first
    Assertions.assertEquals(httpApiFixtures.getIds(aliceMasterSpec, "/versions").size(), 2);

    // each versions is unique
    httpApiFixtures.createVersion(aliceMasterSpec, "v1", 400);

    // only master tenant key can create new Version objects
    httpApiFixtures.createVersion(aliceUserSpec, "2021-03-07", 401);

    // retrieve versions
    List<String> returnedIds = httpApiFixtures.getIds(aliceMasterSpec, "/versions");
    Assertions.assertEquals(List.of(version1.toString(), version2.toString()), returnedIds);
  }

  @Test
  void webhooks_events_create_and_update() {
    //Assertions.assertTrue(fixtures.getIds(aliceUserSpec, "/events").isEmpty());

    JsonObject versionedPayload = new JsonObject();
    JsonObject payload1 = new JsonObject().put("msg", "hello");
    JsonObject payload2 = new JsonObject().put("message", "hello world");
    versionedPayload.put(version1Str, payload1);
    versionedPayload.put(version2Str, payload2);


    UUID id = httpApiFixtures.createWebhook(aliceUserSpec, testData.consumer(), testData.topicStr(), versionedPayload, 201);

    JsonObject resp = httpApiFixtures.getEntity(aliceUserSpec, "/events", id);
    Assertions.assertEquals(resp.getJsonObject("requestBody"), payload1);
  }

}
