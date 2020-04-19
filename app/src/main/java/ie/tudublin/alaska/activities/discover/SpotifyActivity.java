package ie.tudublin.alaska.activities.discover;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import ie.tudublin.alaska.R;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

public class SpotifyActivity extends AppCompatActivity {

    private static String CLIENT_ID;
    private static String REDIRECT_URI;
    private SpotifyAppRemote mSpotifyAppRemote;

    private CardView relaxingCard1, relaxingCard2, inspirationalCard1, inspirationalCard2, pianoCard1, pianoCard2;
    private FrameLayout relaxingPause1, relaxingPause2, inspirationalPause1, inspirationalPause2, pianoPause1, pianoPause2;

    private boolean isPlaying = false;
    private String uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify);

        CLIENT_ID = getString(R.string.spotify_client_id);
        REDIRECT_URI = getString(R.string.spotify_redirect_uri);

        // retrieve view objects
        relaxingCard1 = findViewById(R.id.relaxing_card_1);  // spotify:playlist:37i9dQZF1DXcCnTAt8CfNe
        relaxingCard2 = findViewById(R.id.relaxing_card_2);  // spotify:playlist:6KHFLHMD2rPDqNEirHaibX
        inspirationalCard1 = findViewById(R.id.inspirational_card_1);  // spotify:show:0rgMTVJ9TWDWsut3R1c5L3
        inspirationalCard2 = findViewById(R.id.inspirational_card_2);  // spotify:show:1VXcH8QHkjRcTCEd88U3ti
        pianoCard1 = findViewById(R.id.piano_card_1);  // spotify:playlist:37i9dQZF1DX7K31D69s4M1
        pianoCard2 = findViewById(R.id.piano_card_2);  // spotify:playlist:37i9dQZF1DWVEerxa93vDU

        relaxingPause1 = findViewById(R.id.relaxing_pause_1);
        relaxingPause2 = findViewById(R.id.relaxing_pause_2);
        inspirationalPause1 = findViewById(R.id.inspirational_pause_1);
        inspirationalPause2 = findViewById(R.id.inspirational_pause_2);
        pianoPause1 = findViewById(R.id.piano_pause_1);
        pianoPause2 = findViewById(R.id.piano_pause_2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                connected();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("SpotifyActivity", throwable.getMessage(), throwable);
            }
        });
    }

    private void connected() {
        relaxingCard1.setOnClickListener(v -> {
            if (isPlaying) pause(relaxingPause1);
            else play("spotify:playlist:37i9dQZF1DXcCnTAt8CfNe", relaxingPause1);
        });

        relaxingCard2.setOnClickListener(v -> {
            if (isPlaying) pause(relaxingPause2);
            else play("spotify:playlist:6KHFLHMD2rPDqNEirHaibX", relaxingPause2);
        });

        inspirationalCard1.setOnClickListener(v -> {
            if (isPlaying) pause(inspirationalPause1);
            else play("spotify:show:0rgMTVJ9TWDWsut3R1c5L3", inspirationalPause1);
        });

        inspirationalCard2.setOnClickListener(v -> {
            if (isPlaying) pause(inspirationalPause2);
            else play("spotify:show:1VXcH8QHkjRcTCEd88U3ti", inspirationalPause2);
        });

        pianoCard1.setOnClickListener(v -> {
            if (isPlaying) pause(pianoPause1);
            else play("spotify:playlist:37i9dQZF1DX7K31D69s4M1", pianoPause1);
        });

        pianoCard2.setOnClickListener(v -> {
            if (isPlaying) pause(pianoPause2);
            else play("spotify:playlist:37i9dQZF1DWVEerxa93vDU", pianoPause2);
        });
    }

    private void play(String uri, FrameLayout pauseLayout) {
        isPlaying = true;
        pauseLayout.setTranslationZ(1.0f);
        mSpotifyAppRemote.getPlayerApi().play(uri);
    }

    private void pause(FrameLayout pauseLayout) {
        isPlaying = false;
        pauseLayout.setTranslationZ(0);
        mSpotifyAppRemote.getPlayerApi().pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }
}
