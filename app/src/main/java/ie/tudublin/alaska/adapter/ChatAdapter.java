package ie.tudublin.alaska.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.model.Message;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private int USER = 100;
    private ArrayList<Message> messageArrayList;

    public ChatAdapter(ArrayList<Message> messageArrayList) {
        this.messageArrayList = messageArrayList;
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
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Message message = messageArrayList.get(position);

        switch(message.type) {
            case TEXT:
                ((ViewHolder) holder).message.setText(message.getMessage());
                break;
            case IMAGE:
                ((ViewHolder) holder).message.setVisibility(View.GONE);
                ImageView iv = ((ViewHolder) holder).image;
                Glide.with(iv.getContext())
                    .load(message.getUrl())
                    .into(iv);
        }
    }

    @Override
    public int getItemCount() {
        return messageArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        ImageView image;

        private ViewHolder(View view) {
            super(view);
            message = itemView.findViewById(R.id.message);
            image = itemView.findViewById(R.id.image);
        }
    }
}