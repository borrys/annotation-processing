package annotationprocessing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.tools.Diagnostic.Kind.ERROR;

public class TheProcessor extends AbstractProcessor {

  private final static Pattern UNDERSCORE_PATTERN = Pattern.compile("_([a-z])");

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
          Name enumType = e.getSimpleName();
          PackageElement pack = processingEnv.getElementUtils().getPackageOf(e);

          Map<String, String> enumStrings = processingEnv.getElementUtils().getAllMembers((TypeElement) e)
              .stream()
              .filter(m -> m.getKind() == ElementKind.ENUM_CONSTANT)
              .map(m -> m.getSimpleName().toString())
              .collect(toMap(Function.identity(), toCamelCase()));
          String keyValues = enumStrings.entrySet().stream()
              .map(entry -> format("{0}.{1}, \"{2}\"", enumType, entry.getKey(), entry.getValue()))
              .collect(Collectors.joining(",\n  "));

          String source = new StringBuilder()
              .append("package " + pack + ";\n")
              .append("import java.util.Map;\n")
              .append("public class " + enumType + "Companion {\n")
              .append("  private static final Map<" + enumType + ",String> mapping = Map.of(\n")
              .append(keyValues)
              .append("  );\n")
              .append("  public static String toString(" + enumType + " val) {\n")
              .append("    return mapping.get(val);\n")
              .append("  }\n")
              .append("}\n")
              .toString();
          try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(pack + "." + enumType + "Companion");
            try (var writer = sourceFile.openWriter()) {
              writer.append(source);
            }
          } catch (IOException ex) {
            error(ex.getMessage());
          }
        }
    );

    return true;
  }

  private Function<String, String> toCamelCase() {
    return string -> UNDERSCORE_PATTERN.matcher(string.toLowerCase()).replaceAll(m -> m.group(1).toUpperCase());
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
