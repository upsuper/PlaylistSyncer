package org.upsuper.playlistsyncer.client;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileListAdapter extends BaseAdapter {

	private List<String> fileList;
	private int[] fileSizeList;
	private LayoutInflater inflater;

	private int currentIndex = -1;
	private int currentReceived = 0;

	public FileListAdapter(Context context, List<String> list) {
		fileList = list;
		fileSizeList = new int[list.size()];
		inflater = (LayoutInflater)
				context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return fileList.size();
	}

	@Override
	public String getItem(int position) {
		String file = fileList.get(position);
		int pos = file.lastIndexOf(File.separatorChar);
		return file.substring(pos + 1);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setCurrentPosition(int position, int size) {
		currentIndex = position;
		if (position < fileSizeList.length)
			fileSizeList[position] = size;
		currentReceived = 0;
		notifyDataSetChanged();
	}

	public void updateProgress(int received) {
		currentReceived = received;
		notifyDataSetChanged();
	}

	public static String humanReadableSize(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null)
			v = inflater.inflate(R.layout.view_file_item, parent, false);

		ViewHolder h = (ViewHolder) v.getTag();
		if (h == null) {
			h = new ViewHolder();
			h.imageStatus = (ImageView) v.findViewById(R.id.image_status);
			h.textFilename = (TextView) v.findViewById(R.id.text_filename);
			h.textFilesize = (TextView) v.findViewById(R.id.text_filesize);
			h.textProgress = (TextView) v.findViewById(R.id.text_progress);
			h.progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
			v.setTag(h);
		}

		h.textFilename.setText(getItem(position));
		h.textProgress.setText("");
		h.progressBar.setVisibility(View.INVISIBLE);

		int size = fileSizeList[position];
		if (position <= currentIndex && size > 0) {
			String readableSize = humanReadableSize(size, false);
			h.textFilesize.setText(readableSize);
		} else {
			h.textFilesize.setText("");
		}

		if (position < currentIndex) {
			h.imageStatus.setImageResource(R.drawable.ic_complete);
		} else if (position > currentIndex || size == 0) {
			h.imageStatus.setImageResource(R.drawable.ic_pending);
		} else {
			h.imageStatus.setImageResource(R.drawable.ic_downloading);

			h.progressBar.setVisibility(View.VISIBLE);
			h.progressBar.setMax(size);
			h.progressBar.setProgress(currentReceived);

			int percentage = currentReceived * 100 / size;
			String progress = String.format("%d%%", percentage);
			h.textProgress.setText(progress);
		}

		return v;
	}

	private class ViewHolder {
		public ImageView imageStatus;
		public TextView textFilename;
		public TextView textFilesize;
		public TextView textProgress;
		public ProgressBar progressBar;
	}

}
