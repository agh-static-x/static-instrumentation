package loader;

import instrumentation.OpenTelemetryAgentAdvices;
import instrumentation.PrintingAdvices;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class OpenTelemetryLoader {

    public static URLClassLoader otelClassLoader;
    public static URLClassLoader clientClassLoader;
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

    public static void main(String[] args) throws NoSuchMethodException, IOException {

        loadOtel(new File(System.getProperty("user.dir") + "/src/main/resources/opentelemetry-javaagent-all.jar"));

        new ByteBuddy()
                .rebase(openTelemetryAgentClass)
                .visit(Advice.to(PrintingAdvices.class).on(isMethod()))
                .visit(Advice.to(OpenTelemetryAgentAdvices.class).on(
                        isMethod()
                                .and(named("agentmain")) //or should here be just premain?
                                .and(takesArguments(2))
                        )
                )
                .make() //uncomment next line and comment load and getLoaded to get working example
                .inject(new File(System.getProperty("user.dir") + "/src/main/resources/opentelemetry-javaagent-all.jar"));
//                .load(ClassLoader.getSystemClassLoader())
//                .getLoaded();

//        for(Method method : otelAgent.getMethods()){
//            System.out.println(method.getName());
//        }

//        otelAgent.getDeclaredMethod("premain") //invoke, but on what?

//java -Dota.static.instrumenter=true -javaagent:opentelemetry-java-agent.jar -cp opentelemetry-java-agent.jar:TO-BE-INSTRUMENTED.jar io.opentelemetry.auto.bootstrap.StaticInstrumenter ./tmp


        //TODO: change java home to 15, then try to run StaticInstrumenter

        //TODO: strategies for instrumenting in runtime

    }

}
