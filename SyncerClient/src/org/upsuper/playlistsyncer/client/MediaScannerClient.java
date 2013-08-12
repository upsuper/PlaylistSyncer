package org.upsuper.playlistsyncer.client;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.util.Log;

public class MediaScannerClient implements MediaScannerConnectionClient {

	private MediaScannerConnection scanner;
	private ScannerListener listener;

	private List<String> fileToScan;
	private List<String> fileScanning;

	public MediaScannerClient(Context context) {
		fileToScan = new LinkedList<String>();
		fileScanning = new LinkedList<String>();

		scanner = new MediaScannerConnection(context, this);
		scanner.connect();
	}

	private synchronized void scanFile(String path) {
		scanner.scanFile(path, null);
		fileScanning.add(path);
	}

	public synchronized void scanFile(File file) {
		if (scanner.isConnected()) {
			scanFile(file.getAbsolutePath());
		} else {
			fileToScan.add(file.getAbsolutePath());
		}
	}

	@Override
	public synchronized void onMediaScannerConnected() {
		for (String file : fileToScan)
			scanFile(file);
		fileToScan.clear();
	}

	@Override
	public synchronized void onScanCompleted(String path, Uri uri) {
		if (!fileScanning.remove(path))
			Log.w("MediaScanner", "Unknown path \"" + path + "\" complete.");
		invokeScanComplete();
	}

	private void invokeScanComplete() {
		if (listener != null) {
			if (fileToScan.isEmpty() && fileScanning.isEmpty())
				listener.onScanComplete();
		}
	}

	public synchronized void setScannerListener(ScannerListener listener) {
		this.listener = listener;
		invokeScanComplete();
	}

	public void close() {
		scanner.disconnect();
	}

	public interface ScannerListener {

		public void onScanComplete();

	}

}
