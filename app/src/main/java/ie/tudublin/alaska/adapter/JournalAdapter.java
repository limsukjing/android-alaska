package ie.tudublin.alaska.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
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
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;
    private Context mContext;

    private String entryId;

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
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        View view = LayoutInflater.from(mContext).inflate(R.layout.journal_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setIsRecyclable(false);

        // retrieve data
        entryId = entryList.get(position).entryId;
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

        // settings
        holder.settingsBtn.setOnClickListener(this::createSettingsMenu);
    }


    /**
     * creates a popup menu for journal entry settings that contain two options:
     * edit and delete entry
     * library used: https://github.com/skydoves/PowerMenu
     */
    private void createSettingsMenu(View view) {
        PowerMenu profileMenu = new PowerMenu.Builder(mContext)
                .addItem(new PowerMenuItem(mContext.getResources().getString(R.string.action_edit_entry), false))
                .addItem(new PowerMenuItem(mContext.getResources().getString(R.string.action_delete_entry), false))
                .setAnimation(MenuAnimation.ELASTIC_CENTER) // Animation start point (TOP | LEFT)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .setMenuColor(Color.WHITE)
                .setOnMenuItemClickListener(onMenuItemClick)
                .build();

        profileMenu.showAsDropDown(view);
    }

    private OnMenuItemClickListener<PowerMenuItem> onMenuItemClick = (int position, PowerMenuItem item) -> {
        switch(item.getTitle()) {
            case "Edit Entry":
                Toast.makeText(mContext, "Edit Entry", Toast.LENGTH_SHORT).show();
                break;
            case "Delete Entry":
                Toast.makeText(mContext, "Delete Entry", Toast.LENGTH_SHORT).show();
//                mFirestore.collection("users").document(currentUser.getUid()).collection("journal").document(entryId)
//                        .delete()
//                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                String action = mContext.getResources().getString(R.string.message_action_success, "Entry deleted");
//                                Toast.makeText(mContext, action, Toast.LENGTH_SHORT).show();
//                            }
//                        })
//                        .addOnFailureListener(e -> {
//                            String action = mContext.getResources().getString(R.string.message_action_failure, "delete entry");
//                            Toast.makeText(mContext, action, Toast.LENGTH_SHORT).show();
//                        });
                break;
            default:
                String action = mContext.getResources().getString(R.string.message_error, "An error");
                Toast.makeText(mContext, action, Toast.LENGTH_SHORT).show();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private CircleImageView moodImgView;
        private TextView titleText, dateText, descText;
        private ImageView imgView;
        private ImageButton settingsBtn;


        ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            titleText = mView.findViewById(R.id.entry_title_text);
            dateText = mView.findViewById(R.id.entry_date_text);
            descText = mView.findViewById(R.id.entry_description_text);
            settingsBtn = mView.findViewById(R.id.entry_settings_btn);
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