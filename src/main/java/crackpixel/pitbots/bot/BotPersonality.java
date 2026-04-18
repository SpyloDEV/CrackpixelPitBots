package crackpixel.pitbots.bot;

public final class BotPersonality {

    private final double chaseBias;
    private final double attackTempoMultiplier;
    private final double spacingBias;
    private final double strafeBias;
    private final double aimJitterMultiplier;
    private final double focusBias;
    private final double crowdBias;
    private final double roamBias;
    private final double cadenceBias;
    private final double phaseOffset;

    public BotPersonality(double chaseBias,
                          double attackTempoMultiplier,
                          double spacingBias,
                          double strafeBias,
                          double aimJitterMultiplier,
                          double focusBias,
                          double crowdBias,
                          double roamBias,
                          double cadenceBias,
                          double phaseOffset) {
        this.chaseBias = chaseBias;
        this.attackTempoMultiplier = attackTempoMultiplier;
        this.spacingBias = spacingBias;
        this.strafeBias = strafeBias;
        this.aimJitterMultiplier = aimJitterMultiplier;
        this.focusBias = focusBias;
        this.crowdBias = crowdBias;
        this.roamBias = roamBias;
        this.cadenceBias = cadenceBias;
        this.phaseOffset = phaseOffset;
    }

    public static BotPersonality neutral() {
        return new BotPersonality(1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 0.0D);
    }

    public double getChaseBias() {
        return chaseBias;
    }

    public double getAttackTempoMultiplier() {
        return attackTempoMultiplier;
    }

    public double getSpacingBias() {
        return spacingBias;
    }

    public double getStrafeBias() {
        return strafeBias;
    }

    public double getAimJitterMultiplier() {
        return aimJitterMultiplier;
    }

    public double getFocusBias() {
        return focusBias;
    }

    public double getCrowdBias() {
        return crowdBias;
    }

    public double getRoamBias() {
        return roamBias;
    }

    public double getCadenceBias() {
        return cadenceBias;
    }

    public double getPhaseOffset() {
        return phaseOffset;
    }
}
