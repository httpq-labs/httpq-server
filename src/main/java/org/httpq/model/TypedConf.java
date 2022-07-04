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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public record TypedConf(
  String httpHost,
  int httpPort,
  String dbUrl
) {
  public static TypedConf load() {
    Config config = ConfigFactory.load();
    return new TypedConf(
      config.getString("http.host"),
      config.getInt("http.port"),
      config.getString("database_url")
    );
  }

  public String jdbcUrl() {
    return "jdbc:"+dbUrl;
  }
}
