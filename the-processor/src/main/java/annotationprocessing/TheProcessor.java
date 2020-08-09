package annotationprocessing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
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
    annotations.stream()
        .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
        .map(e -> "@CompileLog found: " + e)
        .forEach(msg -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg));
    return true;
  }
}
