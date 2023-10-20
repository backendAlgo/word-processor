package io.antrex.word_processor.words;

import io.antrex.word_processor.services.FileService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordsHandler implements Handler<RoutingContext> {
  private static final Logger logger = LoggerFactory.getLogger(WordsHandler.class);
  private final FileService fileService;

  public WordsHandler(FileService fileService) {
    this.fileService = fileService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    logger.info("body string {}", routingContext.body().asString());
    String word = routingContext.body().asJsonObject().getString("text");
    fileService.processFile(word)
      .compose(result -> fileService.appendWord(word).map(x -> result))
      .onSuccess(result -> {
        var jsonObject = JsonObject.mapFrom(result);
        routingContext.response()
          .end(jsonObject.toBuffer());
      });
  }
}
