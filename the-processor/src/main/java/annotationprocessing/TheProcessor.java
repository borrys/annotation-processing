package annotationprocessing;

import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
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
import static javax.lang.model.element.Modifier.PUBLIC;
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
        element -> {
          Name enumType = element.getSimpleName();
          PackageElement pack = processingEnv.getElementUtils().getPackageOf(element);

          var companionClass = TypeSpec.classBuilder(enumType + "Companion")
              .addModifiers(PUBLIC)
              .addField(mappingField(pack, (TypeElement) element))
              .addMethod(toStringMethod(pack, (TypeElement) element))
              .build();
          var file = JavaFile.builder(pack.toString(), companionClass).build();

          try {
            file.writeTo(processingEnv.getFiler());
          } catch (IOException ex) {
            error(ex.getMessage());
          }
        }
    );

    return true;
  }

  private FieldSpec mappingField(PackageElement pack, TypeElement e) {
    Map<String, String> enumStrings = processingEnv.getElementUtils().getAllMembers(e)
        .stream()
        .filter(m -> m.getKind() == ElementKind.ENUM_CONSTANT)
        .map(m -> m.getSimpleName().toString())
        .collect(toMap(Function.identity(), toCamelCase()));
    String keyValues = enumStrings.entrySet().stream()
        .map(entry -> format("{0}.{1}, \"{2}\"", e.getSimpleName(), entry.getKey(), entry.getValue()))
        .collect(Collectors.joining(",\n  "));

    ParameterizedTypeName fieldType = ParameterizedTypeName.get(
        ClassName.get(Map.class),
        ClassName.get(pack.toString(), e.getSimpleName().toString()),
        ClassName.get(String.class)
    );
    return FieldSpec.builder(fieldType, "mapping",
        Modifier.PRIVATE,
        Modifier.STATIC,
        Modifier.FINAL
    )
        .initializer("Map.of($L)", keyValues)
        .build();
  }

  private MethodSpec toStringMethod(PackageElement pack, TypeElement e) {
    return MethodSpec.methodBuilder("toString")
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(Modifier.STATIC)
        .addParameter(ClassName.get(pack.toString(), e.getSimpleName().toString()), "val")
        .returns(ClassName.get(String.class))
        .addCode("return mapping.get(val);")
        .build();
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
