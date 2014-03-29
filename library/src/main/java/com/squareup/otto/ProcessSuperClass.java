package com.squareup.otto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows a class to tell otto to look for {@link Produce} and {@link Subscribe} annotated methods in super classes.
 * Recursion only proceeds on classes which are annotated with this annotation, meaning in an inheritance
 * hierarchy each sub-class must have this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProcessSuperClass {
}
