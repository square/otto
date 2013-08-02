/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.otto;

import java.util.Collection;
import java.util.Map;

import com.squareup.otto.EventHandler.EventProducer;
import com.squareup.otto.EventHandler.EventSubscriber;

/** Registry for subscribers, producers and events. */
interface HandlerRegistry {

    Map<Class<?>, EventProducer> findEventProducers(Object listener);
    Map<Class<?>, Collection<EventSubscriber>> findEventSubscribers(Object listener);
    Collection<Class<?>> readEventClassHierarchy(Object event);

    HandlerRegistry ANNOTATED = new HandlerRegistry() {

        @Override
        public Collection<Class<?>> readEventClassHierarchy(Object event) {
            return AnnotatedHandlerRegistry.readEventClassHierarchy(event);
        }

        @Override
        public Map<Class<?>, Collection<EventSubscriber>> findEventSubscribers(Object listener) {
            return AnnotatedHandlerRegistry.findEventSubscribers(listener);
        }

        @Override
        public Map<Class<?>, EventProducer> findEventProducers(Object listener) {
            return AnnotatedHandlerRegistry.findEventProducers(listener);
        }
    };

}
