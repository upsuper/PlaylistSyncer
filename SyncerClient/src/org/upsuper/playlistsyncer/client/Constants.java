package org.upsuper.playlistsyncer.client;

import java.io.File;

import android.os.Environment;

public interface Constants {

	public static final String PACKAGE_NAME = Constants.class.getPackage().getName();
	public static final File MUSIC_DIRECTORY =
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

}
