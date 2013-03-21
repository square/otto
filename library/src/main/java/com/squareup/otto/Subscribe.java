/*
 * Copyright (C) 2007 The Guava Authors
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler, as used by {@link AnnotatedHandlerFinder} and {@link Bus}.
 *
 * <p>The method's first (and only) parameter defines the event type.
 * <p>If this annotation is applied to methods with zero parameters or more than one parameter, the object containing
 * the method will not be able to register for event delivery from the {@link Bus}. Otto fails fast by throwing
 * runtime exceptions in these cases.
 * <p>The subscriber can be executed on a different thread to the poster. The default is {link POSTER_DECIDES}
 * @author Cliff Biffle
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
  /**
   * For safer threading, the subscriber can determine what thread the method is
   * run on.
   */
  public enum ExecuteOn {
    /** The main or UI thread. */ MAIN,
    /** A single threaded background thread. Events are queued. */ BACKGROUND,
    /** One of a thread pool. Events are not queued. */ ASYNC,
    /** Run synchronously on the event poster's thread. */ POSTER_DECIDES
  }

  ExecuteOn thread() default ExecuteOn.POSTER_DECIDES;
}
