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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class EventGenerator<T> {
  Set<T> history;
  Supplier<T> factory;

  public EventGenerator(Supplier<T> factory) {
    this.factory = factory;
    history = new HashSet<>();
  }

  public T make() {
    T event = factory.get();
    history.add(event);
    return event;
  }

  public boolean contains(T event) {
    return history.contains(event);
  }

  public int count() {
    return history.size();
  }

  public Set<T> getAll() {
    return history;
  }
}
