package io.antrex.word_processor.services;

import io.antrex.word_processor.words.Response;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class FileService {
  private final Vertx vertx;

  private static final Logger logger = LoggerFactory.getLogger(FileService.class);
  private final String FILE_PATH = "src/main/resources/words.txt";

  public FileService(Vertx vertx) {
    this.vertx = vertx;
  }

  public Future<Void> appendWord(String word) {
    logger.info("appending word: {}, to file", word);
    Buffer buffer = Buffer.buffer(word + " ");
    OpenOptions openOptions = new OpenOptions().setAppend(true);

    return vertx.fileSystem().open(FILE_PATH, openOptions)
      .compose(file -> file.write(buffer)
        .onComplete(writeResult -> {
          if (writeResult.succeeded()) {
            logger.info("content appended to file");
          } else {
            logger.error("error appending to file");
          }
        }))
      .onSuccess(x -> logger.info("word: {}, is appended to file", word))
      .onFailure(error -> logger.error("error while writing to file", error));
  }

  public Future<Void> createFileIfNotExist() {
    logger.info("checking if file exists");
    return vertx.fileSystem().exists(FILE_PATH)
      .compose(existResult -> {
        if (existResult) {
          return Future.succeededFuture();
        }
        return createFile();
      });
  }

  public Future<Response> processFile(String givenWord) {
    return readWords()
      .map(words -> {
        var value = findClosestWord(givenWord, words);
        var lexicol = findLexicolCloseWord(givenWord, words);
        return new Response(value.orElse(null), lexicol.orElse(null));
      });
  }

  public Future<String[]> readWords() {
    return vertx.fileSystem()
      .readFile(FILE_PATH)
      .map(b -> {
        String fileContent = b.toString();
        return fileContent.split("\\s+");
      });
  }


  private Future<Void> createFile() {
    logger.info("creating file");
    return vertx.fileSystem().createFile(FILE_PATH);
  }


  private Optional<String> findClosestWord(String givenWord, String[] words) {
    givenWord = givenWord.toLowerCase();
    int wordsCharacterValue = calculateCharacterValue(givenWord);
    return Arrays.stream(words)
      .map(word -> new StringToIntegerPair(word,
        Math.abs(wordsCharacterValue - calculateCharacterValue(word))))
      .min(Comparator.comparingInt(a -> a.integer))
      .map(StringToIntegerPair::string);
  }

  private Optional<String> findLexicolCloseWord(String givenWord, String[] words) {
    return Arrays.stream(words)
      .map(word -> new StringToIntegerPair(word, Math.abs(word.compareTo(givenWord))))
      .min(Comparator.comparingInt(a -> a.integer))
      .map(StringToIntegerPair::string);
  }

  private record StringToIntegerPair(String string, Integer integer) {
  }

  private int calculateCharacterValue(String word) {
    return word.chars()
      .filter(Character::isLetter)
      .map(c -> c - 'a' + 1)
      .sum();
  }
}
