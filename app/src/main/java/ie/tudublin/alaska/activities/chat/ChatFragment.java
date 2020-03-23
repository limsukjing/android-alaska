package ie.tudublin.alaska.activities.chat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.assistant.v2.model.DialogNodeOutputOptionsElement;
import com.ibm.watson.assistant.v2.model.RuntimeResponseGeneric;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.adapter.ChatAdapter;
import ie.tudublin.alaska.helper.ClickListener;
import ie.tudublin.alaska.helper.RecyclerTouchListener;
import ie.tudublin.alaska.helper.Util;
import ie.tudublin.alaska.model.Message;

public class ChatFragment extends Fragment {

    private static final int AUDIO_PERMISSION_REQUEST_CODE = 123;

    private SharedPreferences mSharedPreferences;
    private String sharedPrefFile = "ie.tudublin.alaska.sharedPrefFile";

    private ChatAdapter mAdapter;
    private ArrayList<Message> messageArrayList;
    private Util util;
    private Context mContext;

    private RecyclerView chatRecyclerView;
    private EditText inputEditText;
    private ImageButton sendBtn, micBtn;

    private boolean initialRequest;
    private boolean listening = false;
    private MicrophoneInputStream capture;
    private MicrophoneHelper microphoneHelper;

    private Assistant watsonAssistant;
    private Response<SessionResponse> watsonAssistantSession;
    private SpeechToText watsonSTT;
    private TextToSpeech watsonTTS;

    private String permissionGranted, optionSelected;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);

        mContext = getContext();
        util = new Util();

        // retrieve view objects
        chatRecyclerView = root.findViewById(R.id.chat_recycler_view);
        inputEditText = root.findViewById(R.id.chat_input_edit);
        micBtn = root.findViewById(R.id.chat_mic_btn);
        sendBtn = root.findViewById(R.id.chat_send_btn);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList, getContext());
        microphoneHelper = new MicrophoneHelper(getActivity());

        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setItemAnimator(new DefaultItemAnimator());
        chatRecyclerView.setAdapter(mAdapter);

        // initialize EmojiCompat
        FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs);
        EmojiCompat.Config config = new FontRequestEmojiCompatConfig(mContext, fontRequest);
        EmojiCompat.init(config);

        this.inputEditText.setText("");
        this.initialRequest = true;

        permissionGranted = mContext.getResources().getString(R.string.message_permission_granted,"record");

        if(util.isNetworkAvailable(mContext)) {
            chatRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, chatRecyclerView, new ClickListener() {
                @Override
                public void onClick(View view, int position) {
                    Message message = messageArrayList.get(position);

                    if (message != null && !message.getMessage().isEmpty()) {
                        handleRedirect(message.getMessage());
                    }
                }

                @Override
                public void onLongClick(View view, int position) {
                    Message audioMessage = messageArrayList.get(position);

                    if (audioMessage != null && !audioMessage.getMessage().isEmpty()) {
                        new SpeechAsyncTask().execute(audioMessage.getMessage());
                    }
                }
            }));

            sendBtn.setOnClickListener(view -> sendMessage());

            micBtn.setOnClickListener(view -> getRecordAudioPermission());

            createServices();
            sendMessage();
        } else {
            String action = mContext.getResources().getString(R.string.message_error, "Network error");
            Toast.makeText(mContext, action, Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    /**
     * requests record audio permission for STT service
     * the result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private void getRecordAudioPermission() {
        if(ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            recordMessage();
        }
    }

    /**
     * callback for requestPermissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mContext, permissionGranted, Toast.LENGTH_SHORT).show();
                recordMessage();
            } else if ((!ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()), Manifest.permission.RECORD_AUDIO))) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.prompt_audio)
                        .setMessage(R.string.prompt_audio_permission)
                        .setPositiveButton(R.string.action_understand, (dialogInterface, i) -> {
                            Intent settingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            settingIntent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
                            startActivity(settingIntent);
                        })
                        .create()
                        .show();
            } else {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.prompt_audio)
                        .setMessage(R.string.prompt_audio_permission)
                        .setPositiveButton(R.string.action_understand, (dialogInterface, i) -> getRecordAudioPermission())
                        .create()
                        .show();
            }
        }
    }

    /**
     * connects to Watson services i.e. Assistant, TTS and STT
     */
    private void createServices() {
        watsonAssistant = new Assistant("2019-02-28", new IamAuthenticator(mContext.getString(R.string.assistant_api_key)));
        watsonAssistant.setServiceUrl(mContext.getString(R.string.assistant_url));

        watsonTTS = new TextToSpeech(new IamAuthenticator((mContext.getString(R.string.TTS_api_key))));
        watsonTTS.setServiceUrl(mContext.getString(R.string.TTS_url));

        watsonSTT = new SpeechToText(new IamAuthenticator(mContext.getString(R.string.STT_api_key)));
        watsonSTT.setServiceUrl(mContext.getString(R.string.STT_url));
    }

    /**
     * sends user input to Watson Assistant Service
     * handles different types of responses
     * i.e. texts, options and images
     */
    private void sendMessage() {
        String input;

        // Get data
        mSharedPreferences = getActivity().getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE);
        mSharedPreferences.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            mSharedPreferences = sharedPreferences;
            optionSelected = mSharedPreferences.getString(key, "");
            this.inputEditText.setText(optionSelected);
        });

        input = this.inputEditText.getText().toString().trim();

        if (!this.initialRequest) {
            Message inputMessage = new Message();
            inputMessage.setMessage(input);
            inputMessage.setId("1");  // text message
            messageArrayList.add(inputMessage);
        } else {
            Message inputMessage = new Message();
            inputMessage.setMessage(input);
            inputMessage.setId("100");  // user
            this.initialRequest = false;
            Toast.makeText(mContext, R.string.message_tap_voice, Toast.LENGTH_SHORT).show();
        }

        mAdapter.notifyDataSetChanged();

        // clear input and data
        this.inputEditText.setText("");
        mSharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE);
        mSharedPreferences.edit().remove("OPTION_SELECTED").apply();

        Thread thread = new Thread(() -> {
            try {
                if (watsonAssistantSession == null) {
                    ServiceCall<SessionResponse> call = watsonAssistant.createSession(new CreateSessionOptions.Builder().assistantId(mContext.getString(R.string.assistant_id)).build());
                    watsonAssistantSession = call.execute();
                }

                MessageInput messageInput = new MessageInput.Builder()
                        .text(input)
                        .build();
                MessageOptions options = new MessageOptions.Builder()
                        .assistantId(mContext.getString(R.string.assistant_id))
                        .input(messageInput)
                        .sessionId(watsonAssistantSession.getResult().getSessionId())
                        .build();
                Response<MessageResponse> response = watsonAssistant.message(options).execute();

                if(response != null && response.getResult().getOutput() != null && !response.getResult().getOutput().getGeneric().isEmpty()) {
                    List<RuntimeResponseGeneric> responses = response.getResult().getOutput().getGeneric();
                    Log.d("RESPONSES", responses.toString());

                    for(RuntimeResponseGeneric res : responses) {
                        Message outMessage;
                        switch (res.responseType()) {
                            case "text":
                                outMessage = new Message();
                                outMessage.setMessage(res.text());
                                outMessage.setId("2");
                                messageArrayList.add(outMessage);
                                new SpeechAsyncTask().execute(outMessage.getMessage()); // TTS async task for texts
                                break;
                            case "option":
                                outMessage = new Message(res, "option");
                                outMessage.setMessage(res.title() + " " + res.description());
                                outMessage.setOption(res.options());
                                outMessage.setId("2");
                                messageArrayList.add(outMessage);
                                new SpeechAsyncTask().execute(outMessage.getMessage()); // TTS async task for options
                                break;
                            case "image":
                                outMessage = new Message(res, "image");
                                outMessage.setId("2");
                                messageArrayList.add(outMessage);
                                new SpeechAsyncTask().execute("You received an image: " + outMessage.getTitle() + outMessage.getDescription()); // TTS async task for images
                                break;
                            default:
                                Toast.makeText(mContext, R.string.message_chat_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                getActivity().runOnUiThread(() -> {
                    mAdapter.notifyDataSetChanged();
                    if(mAdapter.getItemCount() > 1) {
                        chatRecyclerView.getLayoutManager().smoothScrollToPosition(chatRecyclerView, null, mAdapter.getItemCount() - 1);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }

    /**
     * redirects users to a specific page based on the option selected
     */
    private void handleRedirect(String text) {
        boolean profilePage = text.contains("to the Profile page");
        boolean discoverPage = text.contains("to the Discover page") || text.contains("with the podcasts");
        boolean journalPage = text.contains("to the Journal page") || text.contains("with the journal entry");
        boolean sendIntent = text.contains("with the texts");
        boolean callIntent = text.contains("with the phone call") || text.matches(".* call \\d+\\. Good luck!");
        boolean urlIntent = text.contains("with the walk") || text.contains("with the workout") || text.contains("with the movie") || text.contains("with the café-hopping") || text.contains("with the volunteering activity");

        if (profilePage) {
            NavHostFragment.findNavController(this).navigate(R.id.redirect_profile);
        } else if (discoverPage) {
            NavHostFragment.findNavController(this).navigate(R.id.redirect_discover);
        } else if (journalPage) {
            NavHostFragment.findNavController(this).navigate(R.id.redirect_dashboard);
        } else if (sendIntent) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, "I have a good news!");
            intent.setType("text/plain");
            if (intent.resolveActivity(mContext.getPackageManager()) != null) startActivity(intent);
        } else if (callIntent) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            if (intent.resolveActivity(mContext.getPackageManager()) != null) startActivity(intent);
        } else if (urlIntent) {
            String url = "";
            if (text.contains("with the walk")) {
                url = "https://www.google.com/search?q=weather+forecast&oq=weather+forecast&aqs=chrome..69i57.2269j0j1&sourceid=chrome&ie=UTF-8";
            } else if (text.contains("with the workout")) {
                url = "https://www.google.com/maps/search/nearby+gym/@53.4026474,-6.4084278,14z/data=!3m1!4b1";
            } else if (text.contains("with the movie")) {
                url = "https://www.odeoncinemas.ie/cinemas/blanchardstown/25/";
            } else if (text.contains("with the café-hopping")) {
                url = "https://www.google.com/maps/search/nearby+cafe/@53.4026525,-6.4084278,14z/data=!3m1!4b1";
            } else if (text.contains("with the volunteering activity")) {
                url = "https://www.google.com/maps/search/nearby+volunteer+organization/@53.4026256,-6.4609575,12z/data=!3m1!4b1";
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            if (intent.resolveActivity(mContext.getPackageManager()) != null) startActivity(intent);
        }
    }

    /**
     * records an audio message using a MicrophoneHelper object
     */
    private void recordMessage() {
        if (!listening) {
            capture = microphoneHelper.getInputStream(true);

            new Thread(() -> {
                try {
                    watsonSTT.recognizeUsingWebSocket(getRecognizeOptions(capture), new MicrophoneRecognizeDelegate());
                } catch (Exception e) {
                    showError(e);
                }
            }).start();

            listening = true;
            Toast.makeText(mContext, R.string.message_listening_started, Toast.LENGTH_SHORT).show();
        } else {
            try {
                microphoneHelper.closeInputStream();
                listening = false;
                Toast.makeText(mContext, R.string.message_listening_stopped, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * converts written text into audio using Watson STT Service
     */
    private RecognizeOptions getRecognizeOptions(InputStream audio) {
        return new RecognizeOptions.Builder()
                .audio(audio)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }

    private void showMicText(final String text) {
        getActivity().runOnUiThread(() -> inputEditText.setText(text));
    }

    private void enableMicButton() {
        getActivity().runOnUiThread(() -> micBtn.setEnabled(true));
    }

    private void showError(final Exception e) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        });
    }

    private class SpeechAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            StreamPlayer streamPlayer = new StreamPlayer();

//            streamPlayer.playStream(watsonTTS.synthesize(new SynthesizeOptions.Builder()
//                    .text(params[0])
//                    .voice(SynthesizeOptions.Voice.EN_GB_KATEV3VOICE)
//                    .accept(HttpMediaType.AUDIO_WAV)
//                    .build()).execute().getResult());
            return "Did synthesize";
        }
    }

    /**
     * converts audio into written text using Watson STT Service
     */
    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {
        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            if(speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showMicText(text);
            }
        }

        @Override
        public void onError(Exception e) {
            showError(e);
            enableMicButton();
        }

        @Override
        public void onDisconnected() {
            enableMicButton();
        }
    }
}