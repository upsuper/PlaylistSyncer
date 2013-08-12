package org.upsuper.playlistsyncer.client;

import java.util.HashMap;
import java.util.Map;

import org.upsuper.playlistsyncer.client.R;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class SelectActivity extends Activity
		implements DiscoveryListener, OnItemClickListener, ResolveListener, OnCancelListener {

	public static final String SERVICE_TYPE = "_PlaylistSyncer._tcp.";

	private ArrayAdapter<String> adapter;
	private ListView listService;
	private ProgressDialog dialog;

	private NsdManager nsdManager;
	private Map<String, NsdServiceInfo> serviceMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_select);
		listService = (ListView) findViewById(R.id.list_service);
		adapter = new ArrayAdapter<String>(this, R.layout.view_service_item);
		listService.setAdapter(adapter);
		listService.setOnItemClickListener(this);

		serviceMap = new HashMap<String, NsdServiceInfo>();
		nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

		dialog = new ProgressDialog(this);
		dialog.setIndeterminate(true);
		dialog.setOnCancelListener(this);
		dialog.setMessage("Waiting...");
	}

	@Override
	protected void onResume() {
		super.onResume();
		dialog.show();
		nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		nsdManager.stopServiceDiscovery(this);
		adapter.clear();
		serviceMap.clear();
		dialog.hide();
	}

	@Override
	public void onDiscoveryStarted(String serviceType) {
		// TODO
		Log.d("NSD", "onDiscoveryStarted: " + serviceType);
	}

	@Override
	public void onDiscoveryStopped(String serviceType) {
		// TODO
		Log.d("NSD", "onDiscoveryStopped: " + serviceType);
	}

	@Override
	public void onServiceFound(NsdServiceInfo serviceInfo) {
		final String serviceName = serviceInfo.getServiceName();
		if (!serviceMap.containsKey(serviceName)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.add(serviceName);
					dialog.hide();
				}
			});
		}
		serviceMap.put(serviceName, serviceInfo);
	}

	@Override
	public void onServiceLost(NsdServiceInfo serviceInfo) {
		final String serviceName = serviceInfo.getServiceName();
		serviceMap.remove(serviceName);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.remove(serviceName);
				if (serviceMap.size() == 0)
					dialog.show();
			}
		});
	}

	@Override
	public void onStartDiscoveryFailed(String serviceType, int errorCode) {
		// TODO
		Log.d("NSD", "onStartDiscoveryFailed: " + serviceType + " (" + errorCode + ")");
	}

	@Override
	public void onStopDiscoveryFailed(String serviceType, int errorCode) {
		// TODO
		Log.d("NSD", "onStopDiscoveryFailed: " + serviceType + " (" + errorCode + ")");
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String serviceName = adapter.getItem(position);
		NsdServiceInfo serviceInfo = serviceMap.get(serviceName);
		nsdManager.resolveService(serviceInfo, this);
	}

	@Override
	public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
		Toast.makeText(this, "Resolve failed: " + errorCode, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onServiceResolved(NsdServiceInfo serviceInfo) {
		Intent intent = new Intent(this, SyncActivity.class);
		intent.putExtra(SyncActivity.EXTRA_SERVICE_INFO, serviceInfo);
		startActivity(intent);
		finish();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}

}
