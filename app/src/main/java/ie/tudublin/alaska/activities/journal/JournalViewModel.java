package ie.tudublin.alaska.activities.journal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class JournalViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public JournalViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    public LiveData<String> getText(){
        return mText;
    }
}