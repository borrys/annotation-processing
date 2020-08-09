package annotationprocessing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.text.MessageFormat;
import java.util.Set;

public class TheProcessor extends AbstractProcessor {
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(CompileLog.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    roundEnv.getElementsAnnotatedWith(CompileLog.class)
        .stream()
        .filter(e -> e.getKind() != ElementKind.ENUM)
        .map(element -> MessageFormat.format("{0} only allowed on enums but found on {1} {2}",
            CompileLog.class.getSimpleName(),
            element.getKind(), element))
        .forEach(msg -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg));
    return true;
  }
}
