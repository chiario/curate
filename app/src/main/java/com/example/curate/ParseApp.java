package com.example.curate;
import android.app.Application;
import android.content.Context;

import com.example.curate.models.Like;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.example.curate.models.User;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseUser;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class ParseApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Use for troubleshooting -- remove this line for production
		Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

		// Use for monitoring Parse OkHttp traffic
		// Can be Level.BASIC, Level.HEADERS, or Level.BODY
		// See http://square.github.io/okhttp/3.x/logging-interceptor/ to see the options.
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
		httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
		builder.networkInterceptors().add(httpLoggingInterceptor);

		// Registers Parse Subclasses
		ParseObject.registerSubclass(Song.class);
		ParseObject.registerSubclass(Party.class);
		ParseObject.registerSubclass(PlaylistEntry.class);
		ParseObject.registerSubclass(Like.class);
		ParseUser.registerSubclass(User.class);

		// set applicationId, and server server based on the values in the Heroku settings.
		// clientKey is not needed unless explicitly configured
		// any network interceptors must be added with the Configuration Builder given this syntax
		final Parse.Configuration config = new Parse.Configuration.Builder(this)
				.applicationId(getString(R.string.appId)) // should correspond to APP_ID env variable
				.clientKey(getString(R.string.masterKey))  // set explicitly unless clientKey is explicitly configured on Parse server
				.server(getString(R.string.serverUrl)).build();

		Parse.initialize(config);
	}
}