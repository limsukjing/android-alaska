package ie.tudublin.alaska.model;

import androidx.annotation.NonNull;

public class Tone {
    private double mScore;
    private String mTone;

    public Tone(double score, String tone) {
        mScore = score;
        mTone = tone;
    }

    public String getTone() {
        return mTone;
    }

    public double getScore() {
        return mScore;
    }

    @NonNull
    public String toString() {
        return "Tone: " + mTone + " || Score: " + mScore;
    }
}
