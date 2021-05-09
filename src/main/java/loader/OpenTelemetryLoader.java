package loader;

import instrumentation.*;
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
            .visit(Advice.to(PrintingAdvices.class).on(isMethod()))
            .visit(Advice.to(OpenTelemetryAgentAdvices.class).on(
                    isMethod()
                            .and(named("agentmain"))
                            .and(takesArguments(2))
                    )
            )
            .make()
            .inject(new File(System.getProperty("user.dir") + "/src/main/resources/opentelemetry-javaagent-all.jar"));
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

            String clazzName = clazz.getName().split("\\.")[1];

            new ByteBuddy()
                    .rebase(clazz)
                    .name("io.opentelemetry.javaagent." + clazzName)
                    .make()
                    .inject(new File(System.getProperty("user.dir") + "/src/main/resources/opentelemetry-javaagent-all.jar"));
            System.out.println("Instrumented " + clazzName);
        }

    }

    public static void main(String[] args) throws NoSuchMethodException, IOException {

        loadOtel(new File(System.getProperty("user.dir") + "/src/main/resources/opentelemetry-javaagent-all.jar"));
        instrumentOpenTelemetryAgent();
        injectClasses();


    }

}
