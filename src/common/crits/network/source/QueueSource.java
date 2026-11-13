package crits.network.source;

import crits.waitlist.Waitlists;

@FunctionalInterface
public interface QueueSource extends AutoCloseable {
    Waitlists fetch() throws Exception;
    default void close() { }
}
