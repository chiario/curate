package com.example.curate.utils;

import android.os.Handler;

import com.example.curate.models.Song;

import java.util.ArrayList;
import java.util.List;

public class LiveSearchManager {
    // The minimum time between key presses before a new search request is performed
    private static final int DEBOUNCE_TIME = 50; // milliseconds

    /**
     * Interface used for passing search result data to callback functions
     */
    public interface SearchCallback {
        void onSearchCompleted(String query, List<Song> results);
    }

    /**
     * This class represents a live search request made to the server
     */
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
            // Cancel the request if the query has changed in the debounce time
            if(!query.getQuery().equals(desiredQuery) || isCancelled) {
                mPendingSearches.remove(this);
                return;
            }

            query.find(e -> {
                mPendingSearches.remove(this);

                // Don't run the callback if the search failed or was cancelled
                if(e != null || isCancelled) return;

                // Update the last completed request and run the callback if this request was created
                // after the last completed one
                if(mLastCompletedRequest == null || createdTime > mLastCompletedRequest.createdTime) {
                    mLastCompletedRequest = this;
                    mSearchCallback.onSearchCompleted(query.getQuery(), query.getResults());
                }
            });
        }
    }

    private String desiredQuery; // The current query the user has typed in
    private SearchRequest mLastCompletedRequest; // The most recently completed search request
    private Handler mHandlerThread; // Handler thread for running search requests
    private List<SearchRequest> mPendingSearches; // A list of search requests that have not completed yet
    private SearchCallback mSearchCallback; // Callback to run when a search request completes

    /**
     * @param searchCallback the callback to run when a search request is completed
     */
    public LiveSearchManager(SearchCallback searchCallback) {
        mHandlerThread = new Handler();
        mSearchCallback = searchCallback;
        mPendingSearches = new ArrayList<>();
    }

    /**
     * Cancels all pending searches
     */
    public void cancelPendingSearches() {
        for(SearchRequest request : mPendingSearches) {
            request.isCancelled = true;
        }
        mPendingSearches.clear();
    }

    /**
     * Updates the desired query that the live query is getting results for
     * @param newQuery the new desired query
     */
    public void updateQuery(String newQuery) {
        desiredQuery = newQuery;
        SearchRequest request = new SearchRequest(newQuery, System.currentTimeMillis());
        mPendingSearches.add(request);
        mHandlerThread.postDelayed(request, DEBOUNCE_TIME);
    }

    /**
     * @return true if the live query results are up to date with the desired query
     */
    public boolean isSearchComplete() {
        if(mLastCompletedRequest == null) return false;

        Song.SearchQuery completed = mLastCompletedRequest.query;

        // Return true if the most recently completed search matches the desired query
        return desiredQuery.equals(completed.getQuery());
    }
}
