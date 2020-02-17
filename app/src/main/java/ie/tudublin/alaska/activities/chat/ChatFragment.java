package ie.tudublin.alaska.activities.chat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int RECORD_REQUEST_CODE = 101;

    private ChatAdapter mAdapter;
    private ArrayList<Message> messageArrayList;
    private Util util;
    private Context mContext;

    private RecyclerView chatRecyclerView;
    private EditText inputEditText;
    private ImageButton sendBtn;
    private ImageButton micBtn;

    private boolean initialRequest;
    private boolean listening = false;
    private MicrophoneInputStream capture;
    private MicrophoneHelper microphoneHelper;

    private Assistant watsonAssistant;
    private Response<SessionResponse> watsonAssistantSession;
    private SpeechToText watsonSTT;
    private TextToSpeech watsonTTS;

    private String permissionDenied, permissionGranted;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);

        mContext = getContext();
        util = new Util();

        chatRecyclerView = root.findViewById(R.id.recycler_view_chat);
        inputEditText = root.findViewById(R.id.edit_text_input);
        micBtn = root.findViewById(R.id.btn_mic);
        sendBtn = root.findViewById(R.id.btn_send);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);
        microphoneHelper = new MicrophoneHelper(getActivity());

        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setItemAnimator(new DefaultItemAnimator());
        chatRecyclerView.setAdapter(mAdapter);
        this.inputEditText.setText("");
        this.initialRequest = true;

        permissionDenied = mContext.getResources().getString(R.string.message_permission_denied,"record");
        permissionGranted = mContext.getResources().getString(R.string.message_permission_granted,"record");

        if(util.isNetworkAvailable(mContext)) {
            chatRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext, chatRecyclerView, new ClickListener() {
                @Override
                public void onClick(View view, final int position) {
                    Message audioMessage = messageArrayList.get(position);

                    if (audioMessage != null && !audioMessage.getMessage().isEmpty()) {
                        new SpeechAsyncTask().execute(audioMessage.getMessage());
                    }
                }

                @Override
                public void onLongClick(View view, int position) {
                    recordMessage();
                }
            }));

            sendBtn.setOnClickListener(view -> sendMessage());

            micBtn.setOnClickListener(view -> recordMessage());

            checkRecordAudioPermission();
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
    private void checkRecordAudioPermission() {
        int permission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);

        if(permission != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, permissionDenied, Toast.LENGTH_SHORT).show();

            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, MicrophoneHelper.REQUEST_PERMISSION);
        } else {
            Toast.makeText(mContext, permissionGranted, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * callback for requestPermissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                break;
            case RECORD_REQUEST_CODE: {
                if(grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(mContext, permissionDenied, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, permissionGranted, Toast.LENGTH_SHORT).show();
                }

                return;
            }
            case MicrophoneHelper.REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(mContext, permissionDenied, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * connects to Watson services i.e. Assistant, TTS and STT
     */
    private void createServices() {
        watsonAssistant = new Assistant("2019-02-28", new IamAuthenticator(mContext.getString(R.string.assistant_apikey)));
        watsonAssistant.setServiceUrl(mContext.getString(R.string.assistant_url));

        watsonTTS = new TextToSpeech(new IamAuthenticator((mContext.getString(R.string.TTS_apikey))));
        watsonTTS.setServiceUrl(mContext.getString(R.string.TTS_url));

        watsonSTT = new SpeechToText(new IamAuthenticator(mContext.getString(R.string.STT_apikey)));
        watsonSTT.setServiceUrl(mContext.getString(R.string.STT_url));
    }

    /**
     * sends user input to Watson Assistant Service
     * handles different types of responses
     * i.e. texts, options and images
     */
    private void sendMessage() {
        final String input = this.inputEditText.getText().toString().trim();

        if (!this.initialRequest) {
            Message inputMessage = new Message();
            inputMessage.setMessage(input);
            inputMessage.setId("1");
            messageArrayList.add(inputMessage);
        } else {
            Message inputMessage = new Message();
            inputMessage.setMessage(input);
            inputMessage.setId("100");
            this.initialRequest = false;
            Toast.makeText(mContext, R.string.message_tap_voice, Toast.LENGTH_SHORT).show();
        }

        this.inputEditText.setText("");
        mAdapter.notifyDataSetChanged();

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

                    for(RuntimeResponseGeneric r : responses) {
                        Message outMessage;
                        switch (r.responseType()) {
                            case "text":
                                outMessage = new Message();
                                outMessage.setMessage(r.text());
                                outMessage.setId("2");
                                messageArrayList.add(outMessage);
                                new SpeechAsyncTask().execute(outMessage.getMessage()); // TTS async task for texts
                                break;
                            case "option":
                                outMessage = new Message();
                                String title = r.title();
                                String OptionsOutput = "";
                                for (int i = 0; i < r.options().size(); i++) {
                                    DialogNodeOutputOptionsElement option = r.options().get(i);
                                    OptionsOutput = OptionsOutput + option.getLabel() + "\n";
                                }
                                outMessage.setMessage(title + "\n" + OptionsOutput);
                                outMessage.setId("2");
                                messageArrayList.add(outMessage);
                                new SpeechAsyncTask().execute(outMessage.getMessage()); // TTS async task for options
                                break;
                            case "image":
                                outMessage = new Message(r);
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

            streamPlayer.playStream(watsonTTS.synthesize(new SynthesizeOptions.Builder()
                    .text(params[0])
                    .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
                    .accept(HttpMediaType.AUDIO_WAV)
                    .build()).execute().getResult());
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