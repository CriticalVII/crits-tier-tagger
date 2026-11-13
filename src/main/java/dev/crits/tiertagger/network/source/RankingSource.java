package dev.crits.tiertagger.network.source;

import dev.crits.tiertagger.ranking.Rankings;
import java.time.Instant;

@FunctionalInterface
public interface RankingSource extends AutoCloseable {
    Rankings fetch() throws Exception;
    default void close() { }

    final class ApiException extends Exception {
        private final Instant retryAfter;
        public ApiException(String message, Instant retryAfter) {
            super(message);
            this.retryAfter = retryAfter;
        }
        public Instant retryAfter() { return retryAfter; }
    }
}
