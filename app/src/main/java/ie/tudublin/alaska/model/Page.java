package ie.tudublin.alaska.model;

import ie.tudublin.alaska.R;

public enum Page {
    LOCATION(R.string.title_location, R.layout.discover_location),
    PODCAST(R.string.title_podcast, R.layout.discover_podcast);

    private int mTitleResId;
    private int mLayoutResId;

    Page(int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public int getTitleResId() {
        return mTitleResId;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }
}