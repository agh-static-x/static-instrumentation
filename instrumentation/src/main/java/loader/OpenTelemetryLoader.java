package loader;

import advices.InstallBootstrapJarAdvice;
import advices.OpenTelemetryAgentAdvices;
import advices.PrintingAdvices;
import io.opentelemetry.javaagent.BytesAndName;
import io.opentelemetry.javaagent.PostTransformer;
import io.opentelemetry.javaagent.PreTransformer;
import io.opentelemetry.javaagent.StaticInstrumenter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class OpenTelemetryLoader {

    public static URLClassLoader otelClassLoader;
    public static Class<?> openTelemetryAgentClass;

    public static final String OTEL_AGENT_NAME = "io.opentelemetry.javaagent.OpenTelemetryAgent";
    public static final String OTEL_JAR_PATH = "opentelemetry-javaagent-all.jar";

    public static synchronized void loadOtel(File otelJar){

        try{
            otelClassLoader = new URLClassLoader(
                    new URL[] {otelJar.toURI().toURL()},
                    OpenTelemetryLoader.class.getClassLoader()
            );

            openTelemetryAgentClass = Class.forName(OTEL_AGENT_NAME, true, otelClassLoader);
            System.out.println("Loaded OpenTelemetryAgent: " + openTelemetryAgentClass);

        } catch (MalformedURLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }


    public static void instrumentOpenTelemetryAgent() throws IOException {
            new ByteBuddy()
            .rebase(openTelemetryAgentClass)
//            .visit(Advice.to(PrintingAdvices.class).on(isMethod()))
            .visit(Advice.to(OpenTelemetryAgentAdvices.class).on(
                    isMethod()
                            .and(named("agentmain"))
                            .and(takesArguments(2))
                    )
            )
            .visit(Advice.to(InstallBootstrapJarAdvice.class).on(
                    isMethod()
                            .and(named("installBootstrapJar"))
                    )
            )
            .make()
            .inject(new File(OTEL_JAR_PATH));
//                .load(ClassLoader.getSystemClassLoader())
//                .getLoaded();
    }

    public static void injectClasses() throws IOException {

        List<Class<?>> classesToInject = List.of(
                BytesAndName.class,
                PreTransformer.class,
                PostTransformer.class,
                StaticInstrumenter.class
        );

        for(var clazz : classesToInject) {

            String[] name = clazz.getName().split("\\.");

            String clazzName = name[name.length - 1];

            new ByteBuddy()
                    .rebase(clazz)
                    .name("io.opentelemetry.javaagent." + clazzName)
                    .make()
                    .inject(new File(OTEL_JAR_PATH));
            System.out.println("Instrumented " + clazzName);
        }

    }

    public static void main(String[] args) throws NoSuchMethodException, IOException {

        loadOtel(new File(OTEL_JAR_PATH));
        instrumentOpenTelemetryAgent();
        injectClasses();


    }

}
