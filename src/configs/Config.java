package configs;

/**
 * Interface for a configuration of agents and topics.
 */
public interface Config {
    void create();
    String getName();
    int getVersion();
    void close();
}
