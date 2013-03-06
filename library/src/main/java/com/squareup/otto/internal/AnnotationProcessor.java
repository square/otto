/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.otto.internal;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * Compile-time generation of classes which allow for direct invocation of methods annotated with
 * {@link Subscribe @Subscribe} and {@link Produce @Produce}.
 *
 * @author Jake Wharton
 */
@SupportedAnnotationTypes({ //
    "com.squareup.otto.Produce", //
    "com.squareup.otto.Subscribe" //
})
public class AnnotationProcessor extends AbstractProcessor {

  public static final String PACKAGE_PREFIX = "com.squareup.otto.";
  public static final String FINDER_SUFFIX = "$$Finder";
  private static final String PRODUCER_NAME = "$$Producer$";
  private static final String SUBSCRIBER_NAME = "$$Subscriber$";

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public boolean process(Set<? extends TypeElement> types, RoundEnvironment env) {
    try {
      Map<TypeElement, ExecutableElement> producers = findProducerMethods(env);
      Map<TypeElement, Set<ExecutableElement>> subscribers = findSubscriberMethods(env);

      Set<TypeElement> annotatedTypes = new HashSet<TypeElement>();
      annotatedTypes.addAll(producers.keySet());
      annotatedTypes.addAll(subscribers.keySet());

      for (TypeElement annotatedType : annotatedTypes) {
        writeFinder(annotatedType, producers.get(annotatedType), subscribers.get(annotatedType),
            annotatedTypes);
      }
    } catch (IOException e) {
      error("Unable to complete code generation: " + e.getMessage());
    }
    return true;
  }

  private Map<TypeElement, ExecutableElement> findProducerMethods(RoundEnvironment env) {
    Map<TypeElement, ExecutableElement> producers = new HashMap<TypeElement, ExecutableElement>();
    for (Element element : env.getElementsAnnotatedWith(Produce.class)) {
      TypeElement enclosingType = (TypeElement) element.getEnclosingElement();
      Set<Modifier> typeModifiers = enclosingType.getModifiers();
      if (enclosingType.getKind() != CLASS) {
        error("Unexpected @Produce on " + element);
        continue;
      }
      if (typeModifiers.contains(PRIVATE) || typeModifiers.contains(ABSTRACT)) {
        error("Classes declaring @Produce methods must not be private or abstract: "
            + enclosingType.getQualifiedName());
        continue;
      }

      Set<Modifier> methodModifiers = element.getModifiers();
      if (methodModifiers.contains(PRIVATE) || methodModifiers.contains(ABSTRACT) || methodModifiers
          .contains(STATIC)) {
        error("@Produce methods must not be private, abstract, or static: "
            + enclosingType.getQualifiedName()
            + "."
            + element);
        continue;
      }

      // TODO check signature and verify no args and non-void return type

      if (producers.containsKey(enclosingType)) {
        error("@Produce method for type " + enclosingType + " already registered.");
        continue;
      }
      producers.put(enclosingType, (ExecutableElement) element);
    }

    return producers;
  }

  private Map<TypeElement, Set<ExecutableElement>> findSubscriberMethods(RoundEnvironment env) {
    Map<TypeElement, Set<ExecutableElement>> subscribers =
        new HashMap<TypeElement, Set<ExecutableElement>>();
    for (Element element : env.getElementsAnnotatedWith(Subscribe.class)) {
      TypeElement enclosingType = (TypeElement) element.getEnclosingElement();
      Set<Modifier> typeModifiers = enclosingType.getModifiers();
      if (enclosingType.getKind() != CLASS) {
        error("Unexpected @Subscribe on " + element);
        continue;
      }
      if (typeModifiers.contains(PRIVATE) || typeModifiers.contains(ABSTRACT)) {
        error(
            "Classes declaring @Subscribe methods must not be private or abstract: " + enclosingType
                .getQualifiedName());
        continue;
      }

      Set<Modifier> methodModifiers = element.getModifiers();
      if (methodModifiers.contains(PRIVATE) || methodModifiers.contains(ABSTRACT) || methodModifiers
          .contains(STATIC)) {
        error("@Subscribe methods must not be private, abstract or static: "
            + enclosingType.getQualifiedName()
            + "."
            + element);
        continue;
      }

      // TODO check signature and verify single arg and void return type

      Set<ExecutableElement> methods = subscribers.get(enclosingType);
      if (methods == null) {
        methods = new HashSet<ExecutableElement>();
        subscribers.put(enclosingType, methods);
      }
      methods.add((ExecutableElement) element);
    }

    return subscribers;
  }

  private void writeFinder(TypeElement type, ExecutableElement producer,
      Set<ExecutableElement> subscribers, Set<TypeElement> annotatedTypes) throws IOException {
    String targetClass = type.getQualifiedName().toString();
    String targetClassSimple = targetClass.substring(targetClass.lastIndexOf(".") + 1);
    String className = targetClassSimple + FINDER_SUFFIX;
    String ext = "Object"; // TODO loop to find others in the annotated Set
    String install = "";
    String uninstall = "";
    if (producer != null) {
      String name = producer.getReturnType().toString();
      String simpleName = name.substring(name.lastIndexOf(".") + 1);

      writeProducer(type, targetClass, name, simpleName, producer.getSimpleName().toString());

      install +=
          String.format(INSTALL_PRODUCER, name, targetClassSimple + PRODUCER_NAME + simpleName);
      uninstall += String.format(UNINSTALL_PRODUCER, name);
    }
    if (subscribers != null && !subscribers.isEmpty()) {
      for (ExecutableElement subscriber : subscribers) {
        String name = subscriber.getParameters().get(0).asType().toString();
        String simpleName = name.substring(name.lastIndexOf(".") + 1);

        writeSubscriber(type, targetClass, name, simpleName, subscriber.getSimpleName());

        install += String.format(INSTALL_SUBSCRIBER, name,
            targetClassSimple + SUBSCRIBER_NAME + simpleName);
        uninstall += String.format(UNINSTALL_SUBSCRIBER, name,
            targetClassSimple + SUBSCRIBER_NAME + simpleName);
      }
    }

    JavaFileObject jfo =
        processingEnv.getFiler().createSourceFile(PACKAGE_PREFIX + className, type);
    Writer writer = jfo.openWriter();
    writer.write(String.format(FINDER, className, targetClass, ext, install, uninstall));
    writer.flush();
    writer.close();
  }

  private void writeSubscriber(TypeElement type, String targetClass, String eventClass,
      String simpleName, Name method) throws IOException {
    String className =
        targetClass.substring(targetClass.lastIndexOf(".") + 1) + SUBSCRIBER_NAME + simpleName;
    JavaFileObject jfo =
        processingEnv.getFiler().createSourceFile(PACKAGE_PREFIX + className, type);
    Writer writer = jfo.openWriter();
    writer.write(String.format(SUBSCRIBER, className, targetClass, eventClass, method));
    writer.flush();
    writer.close();
  }

  private void writeProducer(TypeElement type, String targetClass, String eventClass,
      String simpleName, String method) throws IOException {
    String className =
        targetClass.substring(targetClass.lastIndexOf(".") + 1) + PRODUCER_NAME + simpleName;
    JavaFileObject jfo =
        processingEnv.getFiler().createSourceFile(PACKAGE_PREFIX + className, type);
    Writer writer = jfo.openWriter();
    writer.write(String.format(PRODUCER, className, targetClass, eventClass, method));
    writer.flush();
    writer.close();
  }

  private void error(String message) {
    processingEnv.getMessager().printMessage(ERROR, message);
  }

  /**
   * Finder class template. For use with {@link String#format(String, Object...)}.
   *
   * <ol>
   * <li>Class simple name.</li>
   * <li>Target class fully-qualified name.</li>
   * <li>Parent class simple name.</li>
   * <li>Bus installation commands.</li>
   * <li>Bus uninstallation commands.</li>
   * </ol>
   */
  private static final String FINDER = ""
      + "package com.squareup.otto;\n\n"
      + "public class %1$s extends %3$s implements Finder<%2$s> {\n"
      + "  @Override public void install(%2$s instance, BasicBus bus) {\n"
      + "%4$s"
      // No trailing newline.
      + "  }\n\n"
      + "  @Override public void uninstall(%2$s instance, BasicBus bus) {\n"
      + "%5$s"
      // No trailing newline.
      + "  }\n"
      + "}\n";

  private static final String INSTALL_SUBSCRIBER =
      "    bus.installSubscriber(%s.class, new %s(instance));\n";
  private static final String INSTALL_PRODUCER =
      "    bus.installProducer(%s.class, new %s(instance));\n";
  private static final String UNINSTALL_SUBSCRIBER =
      "    bus.uninstallSubscriber(%s.class, new %s(instance));\n";
  private static final String UNINSTALL_PRODUCER = "    bus.uninstallProducer(%s.class);\n";

  /**
   * Subscriber class template. For use with {@link String#format(String, Object...)}.
   *
   * <ol>
   * <li>Class simple name.</li>
   * <li>Target class fully-qualified name.</li>
   * <li>Event class fully-qualified name.</li>
   * <li>Event handling method name.</li>
   * </ol>
   */
  private static final String SUBSCRIBER = ""
      + "package com.squareup.otto;\n\n"
      + "import java.lang.reflect.InvocationTargetException;\n"
      + "import com.squareup.otto.internal.Target;\n\n"
      + "public class %1$s extends Target<%2$s> implements Subscriber<%3$s> {\n"
      + "  private boolean valid = true;\n\n"
      + "  public %1$s(%2$s target) {\n"
      + "    super(target);\n"
      + "  }\n\n"
      + "  @Override public void handle(%3$s event) throws InvocationTargetException {\n"
      + "    if (valid) {\n"
      + "      target.%4$s(event);\n"
      + "    }\n"
      + "  }\n\n"
      + "  @Override public void invalidate() {\n"
      + "    valid = false;\n"
      + "  }\n"
      + "}\n";

  /**
   * Producer class template. For use with {@link String#format(String, Object...)}.
   *
   * <ol>
   * <li>Class simple name.</li>
   * <li>Target class fully-qualified name.</li>
   * <li>Event class fully-qualified name.</li>
   * <li>Event handling method name.</li>
   * </ol>
   */
  private static final String PRODUCER = ""
      + "package com.squareup.otto;\n\n"
      + "import java.lang.reflect.InvocationTargetException;\n"
      + "import com.squareup.otto.internal.Target;\n\n"
      + "public class %1$s extends Target<%2$s> implements Producer<%3$s> {\n"
      + "  private boolean valid = true;\n\n"
      + "  public %1$s(%2$s target) {\n"
      + "    super(target);\n"
      + "  }\n\n"
      + "  @Override public %3$s produce() throws InvocationTargetException {\n"
      + "    return valid ? target.%4$s() : null;\n"
      + "  }\n\n"
      + "  @Override public void invalidate() {\n"
      + "    valid = false;\n"
      + "  }\n"
      + "}\n";
}
