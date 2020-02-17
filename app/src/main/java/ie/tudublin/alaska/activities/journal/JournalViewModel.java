package ie.tudublin.alaska.activities.journal;

import android.app.Activity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

import androidx.lifecycle.ViewModel;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.adapter.JournalAdapter;
import ie.tudublin.alaska.model.JournalEntry;

public class JournalViewModel extends ViewModel {

    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;

    private ProgressBar circularProgress;

    public JournalViewModel() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();
    }

    /**
     * get methods to retrieve journal entry data
     */
    public void retrieveEntryData(View view, Activity activity, List<JournalEntry> entryList, JournalAdapter journalAdapter) {
        circularProgress = view.findViewById(R.id.journal_circular_progress);
        circularProgress.setVisibility(View.VISIBLE);
        entryList.clear();

        if (currentUser != null) {
            Query mQuery = mFirestore.collection("users").document(currentUser.getUid()).collection("journal").orderBy("timestamp", Query.Direction.DESCENDING);

            mQuery.get()
                .addOnSuccessListener(documentSnapshots -> {
                    if (!documentSnapshots.isEmpty()) {
                        for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                            if (doc.getType() == DocumentChange.Type.ADDED) {
                                String entryId = doc.getDocument().getId();
                                JournalEntry entry = doc.getDocument().toObject(JournalEntry.class).withId(entryId);
                                entryList.add(entry);

                                journalAdapter.notifyDataSetChanged();
                                circularProgress.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    String action = activity.getResources().getString(R.string.message_action_failure, "download entry");
                    Toast.makeText(activity, action, Toast.LENGTH_SHORT).show();
                });
        }
    }
}