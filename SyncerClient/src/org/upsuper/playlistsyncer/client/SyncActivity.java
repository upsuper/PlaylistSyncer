package org.upsuper.playlistsyncer.client;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.upsuper.playlistsyncer.client.MediaScannerClient.ScannerListener;
import org.upsuper.playlistsyncer.client.PlaylistUpdater.UpdaterListener;
import org.upsuper.playlistsyncer.client.SyncerClient.ClientListener;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;

import static org.upsuper.playlistsyncer.client.Constants.PACKAGE_NAME;

public class SyncActivity extends Activity
		implements ClientListener, ScannerListener, UpdaterListener {

	public static final String EXTRA_SERVICE_INFO = PACKAGE_NAME + ".SERVICE_INFO";

	SyncerClient client;
	Thread thread;
	List<String> downloadList;
	Map<String, List<String>> playlists;

	MediaScannerClient scanner;

	private ProgressDialog dialog;
	private ListView listFile;
	private FileListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sync);
		listFile = (ListView) findViewById(R.id.list_file);

		Intent intent = getIntent();
		NsdServiceInfo service = (NsdServiceInfo)
				intent.getParcelableExtra(EXTRA_SERVICE_INFO);
		client = new SyncerClient(
				service.getHost(), service.getPort(), this);

		scanner = new MediaScannerClient(this);
		thread = new Thread(client);
		thread.start();
	}

	private void showProgressDialog(String msg) {
		dialog = new ProgressDialog(this);
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.setMessage(msg);
		dialog.show();
	}

	@Override
	public void onConfirmCodeGenerated(final String code) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showProgressDialog("Waiting... Confirm code: " + code);
			}
		});
	}

	@Override
	public void onDownloadListAvailable(final List<String> list) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.dismiss();
				downloadList = list;

				for (String fileId : list)
					Log.d("DList", fileId);
				adapter = new FileListAdapter(SyncActivity.this, list);
				listFile.setAdapter(adapter);
			}
		});
	}

	@Override
	public void onStartDownload(final int index, final int size) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.setCurrentPosition(index, size);
				if (index >= listFile.getFirstVisiblePosition() &&
						index <= listFile.getLastVisiblePosition()) {
					View child = listFile.getChildAt(index);
					int childHeight = child.getHeight();
					int listHeight = listFile.getHeight();
					int top = (listHeight - childHeight) / 2;
					listFile.smoothScrollToPositionFromTop(index, top);
				}
			}
		});
	}

	@Override
	public void onDownloadProgress(final int index, final int received) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.updateProgress(received);
			}
		});
	}

	@Override
	public void onFinishDownload(final int index, File file) {
		scanner.scanFile(file);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.setCurrentPosition(index + 1, 0);
			}
		});
	}

	@Override
	public void onPlaylistsReceived(Map<String, List<String>> playlists) {
		this.playlists = playlists;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showProgressDialog("Updating playlists...");
				// It is necessary to wait for scanning
				// before updating playlists.
				scanner.setScannerListener(SyncActivity.this);
			}
		});
	}

	@Override
	public void onClientError(final Exception exception) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String msg = "Error occurs! " + exception.getMessage();
				Toast.makeText(SyncActivity.this, msg, Toast.LENGTH_LONG).show();
				finish();
			}
		});
	}

	@Override
	public void onScanComplete() {
		scanner.close();
		PlaylistUpdater updater = new PlaylistUpdater(this, this, playlists);
		new Thread(updater).start();
	}

	@Override
	public void onUpdateComplete() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.dismiss();
				String msg = "Sync successfully finished!!";
				Toast.makeText(SyncActivity.this, msg, Toast.LENGTH_LONG).show();
				finish();
			}
		});
	}

}
