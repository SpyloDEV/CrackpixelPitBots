package crackpixel.pitbots.bot;

public class BotSkin {

    private final String value;
    private final String signature;

    public BotSkin(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isValid() {
        return value != null
                && !value.trim().isEmpty()
                && signature != null
                && !signature.trim().isEmpty();
    }
}
