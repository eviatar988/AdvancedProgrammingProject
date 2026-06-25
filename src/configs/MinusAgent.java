package configs;

public class MinusAgent extends BinOpAgent {
    public MinusAgent(String[] subs, String[] pubs) {
        super("minus", subs[0], subs[1], pubs[0], (x, y) -> x - y);
    }
}