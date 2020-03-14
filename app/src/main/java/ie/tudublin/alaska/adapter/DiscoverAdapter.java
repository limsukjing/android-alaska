package ie.tudublin.alaska.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewpager.widget.PagerAdapter;
import ie.tudublin.alaska.R;
import ie.tudublin.alaska.activities.discover.MapsActivity;
import ie.tudublin.alaska.helper.Util;
import ie.tudublin.alaska.model.Page;

public class DiscoverAdapter extends PagerAdapter {

    private Context mContext;
    private Util util;

    private CardView locationCard, podcastCard;

    public DiscoverAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Page page = Page.values()[position];
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(page.getLayoutResId(), container, false);
        container.addView(layout);

        util = new Util();
        if(util.isNetworkAvailable(mContext)) {
            layout.setOnClickListener(view -> {
                // Retrieve view objects
                locationCard = view.findViewById(R.id.location_card);
                podcastCard = view.findViewById(R.id.podcast_card);

                if (locationCard != null) {
                    redirectLocation();
                } else if (podcastCard != null) {
                    redirectPodcast();
                }
            });
        } else {
            String action = mContext.getResources().getString(R.string.message_error, "Network error");
            Toast.makeText(mContext, action, Toast.LENGTH_SHORT).show();
        }

        return layout;
    }

    private void redirectLocation() {
        Intent mapIntent = new Intent(mContext, MapsActivity.class);
        mContext.startActivity(mapIntent);
    }

    private void redirectPodcast() {
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return Page.values().length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        Page pagerEnum = Page.values()[position];

        return mContext.getString(pagerEnum.getTitleResId());
    }
}