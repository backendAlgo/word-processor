package io.antrex.word_processor;

import io.antrex.word_processor.services.FileService;
import io.antrex.word_processor.words.WordsRestApi;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  private FileService fileService;
  private final Integer PORT = 8080;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    fileService = new FileService(vertx);
  }

  @Override
  public void start(Promise<Void> promise) {
    var future = startListening();
    future = future.compose(x -> fileService.createFileIfNotExist());
    future.onComplete(x -> {
      if (x.failed()) {
        logger.error(x.cause().getMessage());
      }
      promise.handle(x);
    });
  }


  private Future<Void> startListening() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create()).failureHandler(handleFailure());

    logger.debug("Creating Routes");
    WordsRestApi.attach(router, fileService);

    return vertx.createHttpServer().requestHandler(router).listen(PORT).onComplete(result -> {
      if (result.succeeded()) {
        logger.info("Listening on port {}", PORT);
      } else {
        logger.error("createHttpServer failed for port {}", PORT, result.cause());
      }
    }).mapEmpty();
  }

  private Handler<RoutingContext> handleFailure() {
    return error -> {
      if (!error.response().ended()) {
        logger.error("Route Error:", error.failure());
        error.response().setStatusCode(500).end(new JsonObject().put("message", "Unknown Error").toBuffer());
      }
    };
  }


  public static void main(String[] args) {
    logger.info("beginning");
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
