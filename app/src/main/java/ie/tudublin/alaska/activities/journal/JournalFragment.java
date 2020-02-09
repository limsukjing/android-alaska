package ie.tudublin.alaska.activities.journal;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.helper.Util;

public class JournalFragment extends Fragment implements View.OnClickListener {

    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 123;

    private JournalViewModel journalViewModel;

    private FloatingActionButton addButton;
    private Button postButton;
    private TextView moodEditText;
    private EditText dateEditText, titleEditText, descEditText;
    private ImageView imgView;
    private DatePickerDialog datePickerDialog;
    private ProgressBar progressBar;
    private LinearLayout joyfulLayout, neutralLayout, sadLayout, anxiousLayout, angryLayout;
    private FrameLayout journalEmptyLayout;
    private BottomSheetDialog dialog;

    private String date, mood, title, desc = null;
    private Uri imageUri = null;

    private StorageReference mStorageRef;
    private FirebaseFirestore mFirestore;
    private DocumentReference docRef;
    private FirebaseUser currentUser;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        journalViewModel = new ViewModelProvider(this).get(JournalViewModel.class);
        View root = inflater.inflate(R.layout.fragment_journal, container, false);

        // retrieve view objects and add a click listener
        addButton = root.findViewById(R.id.journal_fab);
        journalEmptyLayout = root.findViewById(R.id.journal_empty_layout);
        addButton.setOnClickListener(this);

        // initialize Firebase Storage and Firestore
        // retrieve current user
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        checkJournalCollection();

        return root;
    }


    /**
     * retrieve view objects and call setOnClickListener()
     */
    private void initializeViewObject(View dialogView) {
        dateEditText = dialogView.findViewById(R.id.journal_date_edit);
        joyfulLayout = dialogView.findViewById(R.id.journal_joyful);
        neutralLayout = dialogView.findViewById(R.id.journal_neutral);
        sadLayout = dialogView.findViewById(R.id.journal_sad);
        anxiousLayout = dialogView.findViewById(R.id.journal_anxious);
        angryLayout = dialogView.findViewById(R.id.journal_angry);
        moodEditText = dialogView.findViewById(R.id.journal_mood_edit);
        titleEditText = dialogView.findViewById(R.id.journal_title_edit);
        descEditText = dialogView.findViewById(R.id.journal_description_edit);
        imgView = dialogView.findViewById(R.id.journal_img_view);
        postButton = dialogView.findViewById(R.id.journal_post_btn);
        progressBar = dialogView.findViewById(R.id.journal_progress_bar);

        dateEditText.setOnClickListener(this);
        joyfulLayout.setOnClickListener(this);
        neutralLayout.setOnClickListener(this);
        sadLayout.setOnClickListener(this);
        anxiousLayout.setOnClickListener(this);
        angryLayout.setOnClickListener(this);
        imgView.setOnClickListener(this);
        postButton.setOnClickListener(this);
    }


    /**
     * add new journal entry using BottomSheetDialog
     */
    private void newJournalEntry(View view) {
        View dialogView = getLayoutInflater().inflate(R.layout.journal_bottom_sheet, null);
        dialog = new BottomSheetDialog(view.getContext());
        dialog.setContentView(dialogView);
        dialog.show();

        initializeViewObject(dialogView);

        // get current date
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        dateEditText.setText(currentDate);
    }


    /**
     * check if all required fields are filled
     */
    private Boolean isEmpty() {
        // get entries
        date = dateEditText.getText().toString();
        mood = moodEditText.getText().toString();
        title = titleEditText.getText().toString();
        desc = descEditText.getText().toString();

        return date.trim().length() <= 0 || mood.trim().length() <= 0 || title.trim().length() <= 0 || desc.trim().length() <= 0 || imageUri == null;
    }


    /**
     * DatePickerDialog on EditText click event to select a particular date
     */
    private void datePicker(View dialogView) {
        // get current date
        Calendar mCal = Calendar.getInstance();
        int mYear = mCal.get(Calendar.YEAR);
        int mMonth = mCal.get(Calendar.MONTH);
        int mDay = mCal.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(dialogView.getContext(), (view, year, monthOfYear, dayOfMonth) -> {
            // get selected date
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, monthOfYear);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            Date date = cal.getTime();

            String selectedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
            dateEditText.setText(selectedDate);
        }, mYear, mMonth, mDay);

        datePickerDialog.show();
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // disable future dates
    }


    /**
     * Dialog on TextView click event to pick an image from gallery
     * or to capture a photo from the camera
     */
    private void imagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMinCropResultSize(512, 512)
                .setAspectRatio(1, 1)
                .start(getContext(), this);
    }


    /**
     * handles result of the image selection process initiated by imagePicker()
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == Activity.RESULT_OK) {
                imageUri = result.getUri();
                imgView.setImageURI(imageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(getContext(), R.string.message_img_error, Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Add journal entry to Firestore on Button click event
     */
    private void addEntryToFirestore() {
        progressBar.setVisibility(View.VISIBLE);

        Util util = new Util();

        byte[] imageData = util.compressImage(imageUri, getContext(), 720, 720, 50);
        final StorageReference imgRef = mStorageRef.child(currentUser.getUid()).child("journal").child(UUID.randomUUID().toString() + ".jpg");

        // Register observers to listen for when the upload is done or if it fails
        UploadTask uploadImgTask = imgRef.putBytes(imageData);

        uploadImgTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(getContext(), R.string.message_upload_img_error, Toast.LENGTH_LONG).show();
            }

            // Continue with the task to get the download URL
            return imgRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String downloadUri = Objects.requireNonNull(task.getResult()).toString();

                Map<String, Object> data = new HashMap<>();
                data.put("date", date + new SimpleDateFormat(" HH:mm:ss", Locale.getDefault()).format(new Date()));
                data.put("mood", mood);
                data.put("title", title);
                data.put("desc", desc);
                data.put("image_url", downloadUri);

                mFirestore.collection("users").document(currentUser.getUid()).collection("journal").document()
                        .set(data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), R.string.message_upload_entry_success, Toast.LENGTH_LONG).show();

                            // Hide bottom sheet dialog
                            checkJournalCollection();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), R.string.message_upload_entry_error, Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Check if journal entries exist in the database
     */
    private void checkJournalCollection() {
        mFirestore.collection("users").document(currentUser.getUid()).collection("journal").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int size = Objects.requireNonNull(task.getResult()).size();

                if (size == 0) journalEmptyLayout.setVisibility(View.VISIBLE);
                else journalEmptyLayout.setVisibility(View.INVISIBLE);
            } else {
                Toast.makeText(getContext(), R.string.message_unknown_error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Request permission to read external storage of the device
     * The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private void getExternalStoragePermission() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            imagePicker();
        }
    }


    /**
     * Callback for requestPermissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                imagePicker();
            } else {
                getExternalStoragePermission();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.journal_fab:
                newJournalEntry(view);
                break;

            case R.id.journal_date_edit:
                datePicker(view);
                break;

            // LinearLayout click event to select a particular mood
            case R.id.journal_joyful:
                moodEditText.setText(R.string.action_mood_joyful);
                break;
            case R.id.journal_neutral:
                moodEditText.setText(R.string.action_mood_neutral);
                break;
            case R.id.journal_sad:
                moodEditText.setText(R.string.action_mood_sad);
                break;
            case R.id.journal_anxious:
                moodEditText.setText(R.string.action_mood_anxious);
                break;
            case R.id.journal_angry:
                moodEditText.setText(R.string.action_mood_angry);
                break;

            case R.id.journal_img_view:
                getExternalStoragePermission();
                break;

            case R.id.journal_post_btn:
                if(isEmpty()) Toast.makeText(getContext(), R.string.message_input_empty, Toast.LENGTH_SHORT).show();
                else addEntryToFirestore();
                break;
        }
    }
}