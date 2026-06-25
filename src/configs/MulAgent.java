package configs;

public class MulAgent extends BinOpAgent {
    public MulAgent(String[] subs, String[] pubs) {
        super("mul", subs[0], subs[1], pubs[0], (x, y) -> x * y);
    }
}