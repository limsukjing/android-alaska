package ie.tudublin.alaska.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.model.JournalEntry;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.ViewHolder> {

    private List<JournalEntry> entryList;

    private FirebaseStorage mStorage;
    private Context mContext;

    // constructor
    public JournalAdapter(List<JournalEntry> entryList) {
        this.entryList = entryList;
    }

    // getter
    @Override
    public int getItemCount() {
        return entryList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        mStorage = FirebaseStorage.getInstance();

        View view = LayoutInflater.from(mContext).inflate(R.layout.journal_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setIsRecyclable(false);

        // retrieve data
        String mood = entryList.get(position).getMood().toLowerCase();
        String title = entryList.get(position).getTitle();
        String date = entryList.get(position).getDate();
        String description = entryList.get(position).getDescription();
        String imageURL = entryList.get(position).getImageURL();

        // set data
        holder.titleText.setText(title);
        holder.dateText.setText(date);
        holder.descText.setText(description);
        holder.setImage(imageURL);

        // retrieve image url for mood icons
        String img_path = mContext.getResources().getString(R.string.title_mood_img_url, mood);

        mStorage
                .getReference()
                .child(img_path)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> holder.setMoodImage(uri.toString()))
                .addOnFailureListener(exception -> {
                    String action = mContext.getResources().getString(R.string.message_action_failure, "load image");
                    Toast.makeText(mContext, action, Toast.LENGTH_SHORT).show();
                });
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private CircleImageView moodImgView;
        private TextView titleText, dateText, descText;
        private ImageView imgView;

        ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            titleText = mView.findViewById(R.id.entry_title_text);
            dateText = mView.findViewById(R.id.entry_date_text);
            descText = mView.findViewById(R.id.entry_description_text);
        }

        private void setMoodImage(String imageUri){
            moodImgView = mView.findViewById(R.id.entry_mood_img);

            Picasso.get()
                    .load(imageUri)
                    .resize(100, 100)
                    .centerCrop()
                    .placeholder(R.color.tw__light_gray)
                    .into(moodImgView);
        }

        private void setImage(String imageUri){
            imgView = mView.findViewById(R.id.entry_img_view);

            Picasso.get()
                    .load(imageUri)
                    .resize(300, 200)
                    .centerCrop()
                    .placeholder(R.color.tw__light_gray)
                    .into(imgView);
        }
    }
}