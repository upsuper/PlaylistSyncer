package org.upsuper.playlistsyncer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.Media;
import android.util.Log;
import android.util.Pair;

public class PlaylistUpdater implements Runnable {

	private ContentResolver resolver;
	private UpdaterListener listener;
	private Map<String, List<String>> playlists;

	private List<Pair<String, Long>> playlistList;

	public PlaylistUpdater(Context context, UpdaterListener listener,
			Map<String, List<String>> playlists) {
		resolver = context.getContentResolver();
		this.listener = listener;
		this.playlists = playlists;
		playlistList = new ArrayList<Pair<String, Long>>(playlists.size());
	}

	@Override
	public void run() {
		maintainListOfPlaylist();
		for (Pair<String, Long> pl : playlistList)
			updatePlaylist(pl.second, playlists.get(pl.first));
		listener.onUpdateComplete();
	}

	protected void maintainListOfPlaylist() {
		String[] cols = { Playlists._ID, Playlists.NAME };
		Uri uri = Playlists.EXTERNAL_CONTENT_URI;
		Cursor cursor = resolver.query(uri, cols, null, null, null);
		assert cursor != null;
		int idIndex = cursor.getColumnIndex(Playlists._ID);
		int nameIndex = cursor.getColumnIndex(Playlists.NAME);

		Set<String> plToCreate = new HashSet<String>(playlists.keySet());
		List<String> plToDelete = new LinkedList<String>();
		while (cursor.moveToNext()) {
			long id = cursor.getLong(idIndex);
			String name = cursor.getString(nameIndex);
			if (!playlists.containsKey(name)) {
				plToDelete.add(String.valueOf(id));
			} else {
				plToCreate.remove(name);
				playlistList.add(new Pair<String, Long>(name, id));
			}
		}
		cursor.close();

		for (String id : plToDelete)
			resolver.delete(uri, Playlists._ID + "=" + id, null);

		for (String name : plToCreate) {
			ContentValues values = new ContentValues();
			values.put(Playlists.NAME, name);
			Uri rowUri = resolver.insert(uri, values);
			long id = ContentUris.parseId(rowUri);
			playlistList.add(new Pair<String, Long>(name, id));
		}
	}

	protected long getAudioId(String fileId) {
		Cursor cursor = resolver.query(
				Media.EXTERNAL_CONTENT_URI,
				new String[] { Media._ID },
				Media.DATA + " like ?",
				new String[] { "%" + fileId },
				null);
		assert cursor != null && cursor.getCount() == 1;
		cursor.moveToFirst();
		long result = cursor.getLong(cursor.getColumnIndex(Media._ID));
		cursor.close();
		return result;
	}

	protected void updatePlaylist(long playlistId, List<String> members) {
		int total = members.size();
		Map<Long, Long> memberMaps = new HashMap<Long, Long>(total);
		for (int i = 0; i < total; i++) {
			String fileId = members.get(i);
			memberMaps.put(getAudioId(fileId), (long) i);
		}

		Uri uri = Playlists.Members.getContentUri("external", playlistId);
		Cursor cursor = resolver.query(uri,
				new String[] {
					Playlists.Members.AUDIO_ID,
					Playlists.Members.PLAY_ORDER },
				null, null, null);
		assert cursor != null;
		int idIndex = cursor.getColumnIndex(Playlists.Members.AUDIO_ID);
		int orderIndex = cursor.getColumnIndex(Playlists.Members.PLAY_ORDER);

		Set<Long> audioToAdd = new HashSet<Long>(memberMaps.keySet());
		List<String> audioToDelete = new LinkedList<String>();
		while (cursor.moveToNext()) {
			long id = cursor.getLong(idIndex);
			long order = cursor.getLong(orderIndex);
			if (memberMaps.containsKey(id) && order == memberMaps.get(id)) {
				audioToAdd.remove(id);
			} else {
				audioToDelete.add(String.valueOf(id));
			}
		}
		cursor.close();

		Log.d("Playlist", String.format("Total: %d, remove: %d, add: %d",
				members.size(), audioToDelete.size(), audioToAdd.size()));

		for (String id : audioToDelete)
			resolver.delete(uri, Playlists.Members.AUDIO_ID + "=" + id, null);

		int order = 0;
		for (long audio : audioToAdd) {
			ContentValues values = new ContentValues();
			values.put(Playlists.Members.AUDIO_ID, audio);
			values.put(Playlists.Members.PLAY_ORDER, order++);
			resolver.insert(uri, values);
		}
	}

	public interface UpdaterListener {

		public void onUpdateComplete();

	}

}
