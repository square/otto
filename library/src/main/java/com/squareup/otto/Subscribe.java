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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as an event subscriber to a {@link Bus}.
 * <p>
 * The method's first (and only) parameter defines the event type.
 * <p>
 * If this annotation is applied to methods with zero parameters or more than one parameter, the
 * object containing the method will not be able to register for event delivery from the {@link
 * BasicBus}. Otto fails fast by throwing runtime exceptions in these cases.
 *
 * @author Cliff Biffle
 */
@Retention(RUNTIME) @Target(METHOD)
public @interface Subscribe {
}
