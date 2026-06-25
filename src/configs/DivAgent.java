package configs;

public class DivAgent extends BinOpAgent {
    public DivAgent(String[] subs, String[] pubs) {
        super("div", subs[0], subs[1], pubs[0], (x, y) -> y == 0 ? Double.NaN : x / y);
    }
}