package astro;

//import com.google.gson.Gson;

import instrumentation.StaticInstrumenter;
import instrumentation.Transformers;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppRunner {

    public static void main(String[] args) {

        AstroClient client = new AstroClient();

        for(String name : StaticInstrumenter.InstrumentedClasses.keySet()){
            System.out.println("Name=" + name);
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println(client.getSync());
                System.out.println(client.getAsync());
                AppRunner.getAsync();

            } catch (ExecutionException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public static void getAsync(){
        System.out.println("printing just because I can");
    }

}
