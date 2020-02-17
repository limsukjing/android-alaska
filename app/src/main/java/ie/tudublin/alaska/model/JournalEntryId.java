package ie.tudublin.alaska.model;

import com.google.firebase.firestore.Exclude;

import androidx.annotation.NonNull;

public class JournalEntryId {
    @Exclude
    public String entryId;

    @SuppressWarnings("unchecked")
    public <T extends JournalEntryId> T withId(@NonNull final String id) {
        this.entryId = id;
        return (T) this;
    }
}
