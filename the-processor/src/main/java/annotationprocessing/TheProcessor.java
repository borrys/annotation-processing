package annotationprocessing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static javax.tools.Diagnostic.Kind.ERROR;

public class TheProcessor extends AbstractProcessor {
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(ToStringCompanion.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<? extends Element> nonEnums = getAnnotatedNonEnums(roundEnv);
    if (!nonEnums.isEmpty()) {
      error("{0} allowed only on enums, but found on {1}", ToStringCompanion.class.getSimpleName(), nonEnums);
      return true;
    }

    Set<? extends Element> enums = roundEnv.getElementsAnnotatedWith(ToStringCompanion.class);
    enums.forEach(
        e -> {
          Name name = e.getSimpleName();
          PackageElement pack = processingEnv.getElementUtils().getPackageOf(e);
          try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(pack + "." + name + "Companion");
            try (var writer = sourceFile.openWriter()) {
              writer
                  .append("package " + pack + ";\n")
                  .append("import java.util.regex.*;\n")
                  .append("public class " + name + "Companion {\n")
                  .append("  private static final Pattern UNDERSCORE_PATTERN = Pattern.compile(\"_([a-z])\");\n")
                  .append("  public static String toString(" + name + " val) {\n")
                  .append("    Matcher matcher = UNDERSCORE_PATTERN.matcher(val.name().toLowerCase());\n")
                  .append("    return matcher.replaceAll(r -> r.group(1).toUpperCase());\n")
                  .append("  }\n")
                  .append("}\n");
            }

          } catch (IOException ex) {
            error(ex.getMessage());
          }
        }
    );

    return true;
  }

  private List<? extends Element> getAnnotatedNonEnums(RoundEnvironment roundEnv) {
    return roundEnv.getElementsAnnotatedWith(ToStringCompanion.class)
        .stream()
        .filter(e -> e.getKind() != ElementKind.ENUM)
        .collect(toList());
  }

  private void error(String messageTemplate, Object... templateParams) {
    processingEnv.getMessager().printMessage(ERROR, format(messageTemplate, templateParams));
  }
}
