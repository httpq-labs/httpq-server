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
package org.httpq.model;

/**
 *   DELIVERED,
 *   NOT_DELIVERED,
 *   BACKOFF,
 *   UNKNOWN_HOST,
 *   CONNECTION_CLOSED
 */
public enum WebhookState {
  S000("PENDING"),
  S600("DELIVERED"),
  // errors that prevent delivery
  S700("UNDELIVERED_HTTP_ERROR"),
  S701("UNDELIVERED_UNKNOWN_HOST"),
  S703("UNDELIVERED_TIMEOUT"),
  S704("UNDELIVERED_TLS_ERROR"),
  S705("UNDELIVERED_BACKOFF"),
  S799("UNDELIVERED_OTHER");

  private final String state;

  WebhookState(String state) {
    this.state = state;
  }
}
