package com.example.curate.models;

public abstract class SearchItem {
	public static final int TYPE_SONG = 0;
	public static final int TYPE_SECTION = 1;

	abstract public int getType();
}
