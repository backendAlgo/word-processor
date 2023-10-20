package io.antrex.word_processor.words;

import io.antrex.word_processor.services.FileService;
import io.vertx.ext.web.Router;

public class WordsRestApi {
  public static void attach(Router parent, FileService fileService) {
    parent.post("/analyze")
      .handler(new WordsHandler(fileService));
  }
}
