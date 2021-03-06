package advices;
import io.opentelemetry.javaagent.StaticInstrumenter;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;

public class OpenTelemetryAgentAdvices {

    @Advice.OnMethodExit(suppress = Throwable.class)
    static long enter(@Advice.Origin String origin,
                      @Advice.Origin("#t #m") String detailedOrigin,
                      @Advice.Argument(value = 1, readOnly = false) Instrumentation inst) {
//        System.out.println("[INSTRUMENTATION] ENTER " + detailedOrigin);

        inst.addTransformer(StaticInstrumenter.getPostTransformer(), true);
        return System.nanoTime();

    }


}
