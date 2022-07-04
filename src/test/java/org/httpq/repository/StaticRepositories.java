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
package org.httpq.repository;

import io.vertx.sqlclient.SqlConnection;

public class StaticRepositories extends Repositories {

  ConsumerRepository consumerService;
  MainQueueRepository mainQueueService;
  RetryQueueRepository retryQueueService;
  SecurityKeyRepository securityKeyService;
  SubscriptionRepository subscriptionService;
  TenantRepository tenantService;
  TopicRepository topicService;
  VersionRepository versionService;
  WebhookEventRepository webhookEventService;

  public static class Builder {
    ConsumerRepository consumerService;
    MainQueueRepository mainQueueService;
    RetryQueueRepository retryQueueService;
    SecurityKeyRepository securityKeyService;
    SubscriptionRepository subscriptionService;
    TenantRepository tenantService;
    TopicRepository topicService;
    VersionRepository versionService;
    WebhookEventRepository webhookEventService;

    public Builder() {
    }

    public StaticRepositories build() {
      return new StaticRepositories(null, consumerService, mainQueueService, retryQueueService, securityKeyService, subscriptionService, tenantService, topicService, versionService, webhookEventService);
    }
    public Builder setConsumerService(ConsumerRepository consumerService) {
      this.consumerService = consumerService;
      return this;
    }

    public Builder setMainQueueService(MainQueueRepository mainQueueService) {
      this.mainQueueService = mainQueueService;
      return this;
    }

    public Builder setRetryQueueService(RetryQueueRepository retryQueueService) {
      this.retryQueueService = retryQueueService;
      return this;
    }

    public Builder setSecurityKeyService(SecurityKeyRepository securityKeyService) {
      this.securityKeyService = securityKeyService;
      return this;
    }

    public Builder setSubscriptionService(SubscriptionRepository subscriptionService) {
      this.subscriptionService = subscriptionService;
      return this;
    }

    public Builder setTenantService(TenantRepository tenantService) {
      this.tenantService = tenantService;
      return this;
    }

    public Builder setTopicService(TopicRepository topicService) {
      this.topicService = topicService;
      return this;
    }

    public Builder setVersionService(VersionRepository versionService) {
      this.versionService = versionService;
      return this;
    }

    public Builder setWebhookEventService(WebhookEventRepository webhookEventService) {
      this.webhookEventService = webhookEventService;
      return this;
    }
  }

  public StaticRepositories(SqlConnection conn, ConsumerRepository consumerService, MainQueueRepository mainQueueService, RetryQueueRepository retryQueueService, SecurityKeyRepository securityKeyService, SubscriptionRepository subscriptionService, TenantRepository tenantService, TopicRepository topicService, VersionRepository versionService, WebhookEventRepository webhookEventService) {
    super(conn);
    this.consumerService = consumerService;
    this.mainQueueService = mainQueueService;
    this.retryQueueService = retryQueueService;
    this.securityKeyService = securityKeyService;
    this.subscriptionService = subscriptionService;
    this.tenantService = tenantService;
    this.topicService = topicService;
    this.versionService = versionService;
    this.webhookEventService = webhookEventService;
  }

  @Override
  public ConsumerRepository consumer() {
    return this.consumerService;
  }

  @Override
  public MainQueueRepository mainQueue() {
    return this.mainQueueService;
  }

  @Override
  public RetryQueueRepository retryQueue() {
    return this.retryQueueService;
  }

  @Override
  public SecurityKeyRepository securityKey() {
    return this.securityKeyService;
  }

  @Override
  public SubscriptionRepository subscription() {
    return this.subscriptionService;
  }

  @Override
  public TenantRepository tenant() {
    return this.tenantService;
  }

  @Override
  public TopicRepository topic() {
    return this.topicService;
  }

  @Override
  public VersionRepository version() {
    return this.versionService;
  }

  @Override
  public WebhookEventRepository webhookEvent() {
    return this.webhookEventService;
  }
}
