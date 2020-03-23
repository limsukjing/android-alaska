package ie.tudublin.alaska.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ibm.watson.assistant.v2.model.DialogNodeOutputOptionsElement;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.model.Message;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private int USER = 100;
    private ArrayList<Message> messageArrayList;
    private Context context;

    private SharedPreferences mSharedPreferences;
    private String sharedPrefFile = "ie.tudublin.alaska.sharedPrefFile";

    public ChatAdapter(ArrayList<Message> messageArrayList, Context context) {
        this.messageArrayList = messageArrayList;
        this.context = context;
    }

    @Override @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;

        // view type is to identify where to render the chat message
        if (viewType == USER) {
            // user's messages
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_user, parent, false);
        } else {
            // Watson's messages
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_watson, parent, false);
        }

        return new ViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageArrayList.get(position);

        if(message.getId() != null && message.getId().equals("1")) {
            return USER;
        }

        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        TextView text = ((ViewHolder) holder).message;
        LinearLayout buttonLayout = ((ViewHolder) holder).button;
        ImageView image = ((ViewHolder) holder).image;

        Message message = messageArrayList.get(position);

        switch(message.type) {
            case TEXT:
                if (buttonLayout != null) {
                    buttonLayout.setVisibility(View.GONE);
                }

                text.setText(message.getMessage());
                break;
            case OPTION:
                if (buttonLayout != null) {
                    text.setText(message.getMessage());

                    List<DialogNodeOutputOptionsElement> optionList = message.getOption();

                    for (int i = 0; i < optionList.size(); i++) {
                        DialogNodeOutputOptionsElement option = optionList.get(i);
                        createButton(i, option, buttonLayout);
                    }

                    optionList.clear();
                }
                break;
            case IMAGE:
                if (image != null) {
                    text.setVisibility(View.GONE);
                    Glide.with(image.getContext())
                            .load(message.getUrl())
                            .into(image);
                }
                break;
        }
    }


    /**
     * displays options returned from the API as dynamic buttons
     */
    private void createButton(int i, DialogNodeOutputOptionsElement option, LinearLayout buttonLayout) {
        // create buttons and layouts
        Button button = new Button(context);
        button.setId(i+1);
        button.setText(option.getLabel());
        button.setTextSize(10);
        button.setPadding(0,0,0,0);
        button.setBackground(context.getDrawable(R.drawable.background_watson_option));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 10);
        button.setLayoutParams(params);
        buttonLayout.addView(button);

        button.setOnClickListener(v -> {
            mSharedPreferences = context.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("OPTION_SELECTED", option.getValue().getInput().text());
            editor.apply();
        });
    }

    @Override
    public int getItemCount() {
        return messageArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        LinearLayout button;
        ImageView image;

        private ViewHolder(View view) {
            super(view);
            message = itemView.findViewById(R.id.message);
            button = itemView.findViewById(R.id.message_btn_layout);
            image = itemView.findViewById(R.id.image);
        }
    }
}