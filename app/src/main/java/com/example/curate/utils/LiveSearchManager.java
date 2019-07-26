package com.example.curate.utils;

import android.os.Handler;

import com.example.curate.models.Song;

import java.util.ArrayList;
import java.util.List;

public class LiveSearchManager {
    private static final int DEBOUNCE_TIME = 50; // milliseconds

    private class SearchRequest implements Runnable {
        Song.SearchQuery query;
        long createdTime;
        boolean isCancelled;

        private SearchRequest(String query, long timestamp) {
            this.createdTime = timestamp;
            this.isCancelled = false;
            this.query = new Song.SearchQuery()
                    .setQuery(query)
                    .setIsLive(true)
                    .setLimit(30);
        }

        @Override
        public void run() {
            if(!query.getQuery().equals(desiredQuery) || isCancelled) {
                mPendingSearches.remove(this);
                return;
            }

            query.find(e -> {
                mPendingSearches.remove(this);

                if(e != null || isCancelled) {
                    return;
                }

                if(mostRecentlyCompleted == null || createdTime > mostRecentlyCompleted.createdTime) {
                    mostRecentlyCompleted = this;
                    mSearchCallback.onSearchCompleted(query.getQuery(), query.getResults());
                }
            });
        }
    }

    public interface SearchCallback {
        void onSearchCompleted(String query, List<Song> results);
    }

    private String desiredQuery;
    private SearchRequest mostRecentlyCompleted;
    private Handler mHandlerThread;
    private List<SearchRequest> mPendingSearches;
    private SearchCallback mSearchCallback;

    public LiveSearchManager(SearchCallback searchCallback) {
        mHandlerThread = new Handler();
        mSearchCallback = searchCallback;
        mPendingSearches = new ArrayList<>();
    }

    public void cancelPendingSearches() {
        for(SearchRequest request : mPendingSearches) {
            request.isCancelled = true;
        }
        mPendingSearches.clear();
    }

    public void updateQuery(String newQuery) {
        desiredQuery = newQuery;
        SearchRequest request = new SearchRequest(newQuery, System.currentTimeMillis());
        mPendingSearches.add(request);
        mHandlerThread.postDelayed(request, DEBOUNCE_TIME);
    }

    public boolean isSearchComplete() {
        if(!desiredQuery.equals(mostRecentlyCompleted.query.getQuery())) return false;
        if(desiredQuery.isEmpty()) return true;
        if(mostRecentlyCompleted.query.getResults().isEmpty()) return false;
        return true;
    }

}
