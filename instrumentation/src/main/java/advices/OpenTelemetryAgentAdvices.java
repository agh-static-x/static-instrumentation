package advices;
import instrumentation.StaticInstrumenter;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;

public class OpenTelemetryAgentAdvices {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    static long enter(@Advice.Origin String origin,
                      @Advice.Origin("#t #m") String detailedOrigin,
                      @Advice.Argument(value = 1, readOnly = false) Instrumentation inst) {
//        System.out.println("[INSTRUMENTATION] ENTER " + detailedOrigin);

        inst.addTransformer(StaticInstrumenter.getPreTransformer());
        return System.nanoTime();

    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    static void exit(@Advice.Origin String origin,
                     @Advice.Origin("#t #m") String detailedOrigin,
                     @Advice.Argument(value = 1, readOnly = false) Instrumentation inst) {
//        System.out.println("[INSTRUMENTATION] EXIT " + detailedOrigin);
        inst.addTransformer(StaticInstrumenter.getPostTransformer());
    }
}
