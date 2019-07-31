package com.example.curate.utils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.curate.models.PlaylistEntry;

import java.util.List;

public class EntryListDiffCallback extends DiffUtil.Callback {
    private List<PlaylistEntry> mOldList;
    private List<PlaylistEntry> mNewList;

    public EntryListDiffCallback(List<PlaylistEntry> oldList, List<PlaylistEntry> newList) {
        this.mOldList = oldList;
        this.mNewList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldList != null ? mOldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return mNewList != null ? mNewList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mNewList.get(newItemPosition).equals(mOldList.get(oldItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mNewList.get(newItemPosition).contentsEqual(mOldList.get(oldItemPosition));
    }
}
