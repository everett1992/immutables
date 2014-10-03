package org.immutables.modeling;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.Writer;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateNested;
import org.immutables.modeling.Templates.CharConsumer;
import org.immutables.modeling.Templates.Invokable;
import org.immutables.modeling.Templates.Invokation;

@GenerateNested
public final class Output {

  public Templates.Invokable java() {
    return invokable;
  }

  private static Joiner DOT_JOINER = Joiner.on('.');

  private final Templates.Invokable invokable = new Templates.Fragment(3) {
    @Override
    public void run(Invokation invokation) {
      String packageName = invokation.param(0).toString();
      String simpleName = invokation.param(1).toString();
      Invokable body = (Invokable) invokation.param(2);

      ImmutableOutput.JavaFileKey key = ImmutableOutput.JavaFileKey.builder()
          .packageName(packageName)
          .simpleName(simpleName)
          .build();

      body.invoke(new Invokation(getFiles().files.getUnchecked(key).consumer, invokation.indentation()));
    }
  };

  @GenerateImmutable
  public static abstract class JavaFileKey {
    public abstract String packageName();

    public abstract String simpleName();

    @Override
    public String toString() {
      return DOT_JOINER.join(packageName(), simpleName());
    }
  }

  private static class JavaFile {
    final JavaFileKey key;
    final Templates.StringBuilderConsumer consumer = new Templates.StringBuilderConsumer();

    public JavaFile(JavaFileKey key) {
      this.key = key;
    }

    public void complete() {
      try {
        try (Writer writer = getFiler().createSourceFile(key.toString()).openWriter()) {
          writer.append(extractSourceCode());
        }
      } catch (FilerException ex) {
        throw Throwables.propagate(ex);
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
    }

    @SuppressWarnings("deprecation")
    private CharSequence extractSourceCode() {
      return LegacyJavaPostprocessing.rewrite(consumer.asCharSequence());
    }
  }

  private static Filer getFiler() {
    return StaticEnvironment.processing().getFiler();
  }

  private static Files getFiles() {
    return StaticEnvironment.getInstance(Files.class, FilesSupplier.INSTANCE);
  }

  private enum FilesSupplier implements Supplier<Files> {
    INSTANCE;
    @Override
    public Files get() {
      return new Files();
    }
  }

  private static class Files implements StaticEnvironment.Completable {
    final LoadingCache<JavaFileKey, JavaFile> files = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(new CacheLoader<JavaFileKey, JavaFile>() {
          @Override
          public JavaFile load(JavaFileKey key) throws Exception {
            return new JavaFile(key);
          }
        });

    @Override
    public void complete() {
      for (JavaFile file : files.asMap().values()) {
        file.complete();
      }
    }
  }
}
