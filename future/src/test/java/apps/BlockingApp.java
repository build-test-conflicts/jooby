package apps;

import io.jooby.App;
import io.jooby.Mode;
import io.jooby.utow.Utow;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class BlockingApp extends App {
  {
    get("/", ctx -> {
      System.out.println("Scheduled: " + Thread.currentThread().getName());
      return Thread.currentThread().getName();
    });
  }

  public static void main(String[] args) {
    new Utow()
        .deploy(new BlockingApp().mode(Mode.EVENT_LOOP))
        .start()
        .join();
  }
}
