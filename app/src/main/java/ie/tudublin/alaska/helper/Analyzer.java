package ie.tudublin.alaska.helper;

import android.content.Context;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.tone_analyzer.v3.model.ToneOptions;

import java.util.ArrayList;

import ie.tudublin.alaska.R;
import ie.tudublin.alaska.model.Tone;

public class Analyzer {

    private Context mContext;
    private ToneAnalyzer toneAnalyzer;
    private String mText;

    public Analyzer(Context context) {
        mContext = context;
        toneAnalyzer = new ToneAnalyzer("2016-05-19", new IamAuthenticator(context.getString(R.string.analyzer_api_key)));
        toneAnalyzer.setServiceUrl(context.getString(R.string.analyzer_url));
        mText = "";
    }

    public void setText(String text) {
        mText = text;
    }

    public String getToneResult() {
        ToneOptions toneOptions = new ToneOptions.Builder()
                .text(mText)
                .build();

        ToneAnalysis tone = toneAnalyzer.tone(toneOptions).execute().getResult();
        ArrayList<Tone> toneList = analyzeTone(tone.toString());

        return getHighestTone(toneList).getTone();
    }

    private static ArrayList<Tone> analyzeTone(String tone) {
        ArrayList<Tone> res = new ArrayList<>();
        ArrayList<Integer> index = new ArrayList<>();
        ArrayList<Integer> index2 = new ArrayList<>();
        ArrayList<Integer> index3 = new ArrayList<>();
        ArrayList<Integer> index4 = new ArrayList<>();

        int curr = tone.indexOf("score");
        while (curr >= 0) {
            index.add(curr);
            curr = tone.indexOf("score", curr+1);
        }

        curr = tone.indexOf(",", index.get(0));
        for (int i = 1; i < index.size(); i++) {
            index2.add(curr);
            curr = tone.indexOf(",", index.get(i)+1);
        }

        curr = tone.indexOf(",", index.get(index.size()-1)+1);
        index2.add(curr);

        curr = tone.indexOf("tone_id");
        while (curr >= 0) {
            index3.add(curr);
            curr = tone.indexOf("tone_id", curr+1);
        }

        curr = tone.indexOf(",", index3.get(0));
        for (int i = 1; i < index3.size(); i++) {
            index4.add(curr);
            curr = tone.indexOf(",", index3.get(i)+1);
        }

        curr = tone.indexOf(",", index3.get(index3.size()-1)+1);
        index4.add(curr);

        for (int i = 0; i < index.size(); i++) {
            String score = tone.substring(index.get(i), index2.get(i));
            score = score.substring(score.indexOf(":")+1);
            double scoreNum = Double.parseDouble(score);

            String toneStr = tone.substring(index3.get(i), index4.get(i));
            toneStr = toneStr.replace("\"", "");
            toneStr = toneStr.substring(toneStr.indexOf(":")+1);
            toneStr = toneStr.trim();

            res.add(new Tone(scoreNum, toneStr));
        }

        return res;
    }

    private static Tone getHighestTone(ArrayList<Tone> toneList) {
        double maxScore = 0;
        int maxScoreIndex = 0;

        for (int i = 0; i < toneList.size(); i++) {
            if (toneList.get(i).getScore() > maxScore) {
                maxScore = toneList.get(i).getScore();
                maxScoreIndex = i;
            }
        }

        return toneList.get(maxScoreIndex);
    }
}
