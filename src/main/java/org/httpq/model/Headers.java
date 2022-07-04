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

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;

public record Headers(List<Header> list){
  public static Headers NO_HEADERS = new Headers(List.of());
  public static Headers deserialize(String s) {
    return Optional.ofNullable(s)
      .map(JsonObject::new)
      .map(o -> o.mapTo(Headers.class))
      .orElse(null);
  }

  public String serialize() {
    return JsonObject.mapFrom(this).encode();
  }
}
