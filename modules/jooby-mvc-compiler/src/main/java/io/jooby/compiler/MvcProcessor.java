/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.compiler;

import com.google.auto.service.AutoService;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(Processor.class)
public class MvcProcessor implements Processor {

  private Map<String, MvcHandlerCompiler> result = new HashMap<>();

  private ProcessingEnvironment processingEnvironment;

  @Override public Set<String> getSupportedOptions() {
    return Collections.emptySet();
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    return Annotations.HTTP_METHODS;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override public void init(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnvironment) {
    if (annotations == null || annotations.size() == 0) {
      return false;
    }
    for (TypeElement httpMethod : annotations) {
      Set<? extends Element> methods = roundEnvironment.getElementsAnnotatedWith(httpMethod);
      for (Element e : methods) {
        ExecutableElement method = (ExecutableElement) e;
        MvcHandlerCompiler compiler = new MvcHandlerCompiler(processingEnvironment, httpMethod.getSimpleName().toString(), method);
        String key = compiler.getKey();
        result.put(key, compiler);
      }
    }

    return true;
  }

  /*package*/ MvcHandlerCompiler compilerFor(String methodDescriptor) {
    return result.get(methodDescriptor);
  }

  private List<String> path(TypeElement method, ExecutableElement exec) {
    List<String> prefix = path(exec.getEnclosingElement());
    // Favor GET("/path") over Path("/path") at method level
    List<String> path = path(method.getQualifiedName().toString(), method.getAnnotationMirrors());
    if (path.size() == 0) {
      path = path(method.getQualifiedName().toString(), exec.getAnnotationMirrors());
    }
    List<String> methodPath = path;
    if (prefix.size() == 0) {
      return path;
    }
    if (path.size() == 0) {
      return prefix;
    }
    return prefix.stream()
        .flatMap(root -> methodPath.stream().map(p -> root + p))
        .collect(Collectors.toList());
  }

  private List<String> path(Element element) {
    return path(null, element.getAnnotationMirrors());
  }

  private List<String> path(String method, List<? extends AnnotationMirror> annotations) {
    return annotations.stream()
        .map(AnnotationMirror.class::cast)
        .flatMap(mirror -> {
          String type = mirror.getAnnotationType().toString();
          if (type.equals(Annotations.PATH) || type.equals(method)) {
            return annotationAttribute(mirror, "value").stream();
          }
          return Stream.empty();
        })
        .collect(Collectors.toList());
  }

  private List<String> annotationAttribute(AnnotationMirror mirror, String name) {
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
        .getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(name)) {
        Object value = entry.getValue().getValue();
        if (value instanceof List) {
          List values = (List) value;
          return (List<String>) values.stream()
              .map(it -> cleanString(it.toString()))
              .collect(Collectors.toList());
        }
        return Collections.singletonList(cleanString(value.toString()));
      }
    }
    return Collections.emptyList();
  }

  private String cleanString(String value) {
    if (value.length() > 0 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
      ExecutableElement member, String userText) {
    return Collections.emptyList();
  }
}