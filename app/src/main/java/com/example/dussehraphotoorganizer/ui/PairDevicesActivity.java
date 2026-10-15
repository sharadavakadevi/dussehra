package com.example.dussehraphotoorganizer.ui;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.sync.data.SyncLedgerRepository;
import com.example.dussehraphotoorganizer.sync.engine.SyncEngine;
import com.example.dussehraphotoorganizer.sync.net.SyncPacket;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PairDevicesActivity extends AppCompatActivity {

    private TextView tvSyncStatusBadge;
    private TextView tvWifiInfo;
    private TextView tvSyncDetail;
    private ProgressBar progressBarSync;
    private TextView tvProgressFile;

    private TextView tvThisDeviceName;
    private TextView tvThisDeviceIp;
    private TextView tvOwnPairingPin;
    private MaterialButton btnRegeneratePin;

    private LinearLayout layoutPairedInfo;
    private TextView tvPairedDeviceName;
    private TextView tvPairedDeviceIp;
    private TextView tvLastSyncTime;
    private MaterialButton btnTestSyncNow;
    private MaterialButton btnUnpair;

    private LinearLayout layoutUnpairedInfo;
    private MaterialButton btnPairNewDevice;

    private TextView tvSentCount;
    private TextView tvReceivedCount;
    private TextView tvEmptyLogs;
    private RecyclerView rvSyncLogs;

    private SyncEngine syncEngine;
    private SyncLedgerRepository ledgerRepository;
    private SyncLogAdapter logAdapter;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_devices);

        MaterialToolbar toolbar = findViewById(R.id.toolbarPairDevices);
        toolbar.setNavigationOnClickListener(v -> finish());

        syncEngine = SyncEngine.getInstance(this);
        ledgerRepository = syncEngine.getLedgerRepository();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        updateUiState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncEngine.addListener(syncEngineListener);
        updateUiState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        syncEngine.removeListener(syncEngineListener);
    }

    private void initViews() {
        tvSyncStatusBadge = findViewById(R.id.tvSyncStatusBadge);
        tvWifiInfo = findViewById(R.id.tvWifiInfo);
        tvSyncDetail = findViewById(R.id.tvSyncDetail);
        progressBarSync = findViewById(R.id.progressBarSync);
        tvProgressFile = findViewById(R.id.tvProgressFile);

        tvThisDeviceName = findViewById(R.id.tvThisDeviceName);
        tvThisDeviceIp = findViewById(R.id.tvThisDeviceIp);
        tvOwnPairingPin = findViewById(R.id.tvOwnPairingPin);
        btnRegeneratePin = findViewById(R.id.btnRegeneratePin);

        layoutPairedInfo = findViewById(R.id.layoutPairedInfo);
        tvPairedDeviceName = findViewById(R.id.tvPairedDeviceName);
        tvPairedDeviceIp = findViewById(R.id.tvPairedDeviceIp);
        tvLastSyncTime = findViewById(R.id.tvLastSyncTime);
        btnTestSyncNow = findViewById(R.id.btnTestSyncNow);
        btnUnpair = findViewById(R.id.btnUnpair);

        layoutUnpairedInfo = findViewById(R.id.layoutUnpairedInfo);
        btnPairNewDevice = findViewById(R.id.btnPairNewDevice);

        tvSentCount = findViewById(R.id.tvSentCount);
        tvReceivedCount = findViewById(R.id.tvReceivedCount);
        tvEmptyLogs = findViewById(R.id.tvEmptyLogs);
        rvSyncLogs = findViewById(R.id.rvSyncLogs);
    }

    private void setupRecyclerView() {
        rvSyncLogs.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new SyncLogAdapter();
        rvSyncLogs.setAdapter(logAdapter);
    }

    private void setupClickListeners() {
        btnRegeneratePin.setOnClickListener(v -> {
            String newPin = ledgerRepository.regenerateOwnPairingPin();
            tvOwnPairingPin.setText(newPin);
            Toast.makeText(this, "New 6-digit PIN generated: " + newPin, Toast.LENGTH_SHORT).show();
        });

        btnTestSyncNow.setOnClickListener(v -> {
            Toast.makeText(this, "Testing connection and starting photo sync...", Toast.LENGTH_SHORT).show();
            syncEngine.triggerSync();
        });

        btnUnpair.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Unpair Device?")
                    .setMessage("Do you want to unpair " + ledgerRepository.getPairedDeviceName() + "? Automatic transfers will stop until you pair again.")
                    .setPositiveButton("Unpair", (dialog, which) -> {
                        ledgerRepository.unpair();
                        updateUiState();
                        Toast.makeText(this, "Device unpaired.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnPairNewDevice.setOnClickListener(v -> showPairingDialog());
    }

    private void showPairingDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView tvInstructions = new TextView(this);
        tvInstructions.setText("Enter the IP Address and 6-Digit PIN shown on the other phone's screen:");
        tvInstructions.setTextSize(14);
        tvInstructions.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        layout.addView(tvInstructions);

        EditText etIp = new EditText(this);
        etIp.setHint("Other Phone IP (e.g. 192.168.1.102)");
        etIp.setInputType(InputType.TYPE_CLASS_TEXT);
        String lastIp = ledgerRepository.getLastKnownIp();
        if (lastIp != null && !lastIp.isEmpty()) etIp.setText(lastIp);
        layout.addView(etIp);

        EditText etPin = new EditText(this);
        etPin.setHint("6-Digit Pairing PIN");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPin);

        new AlertDialog.Builder(this)
                .setTitle("Pair with Phone")
                .setView(layout)
                .setPositiveButton("Pair & Connect", (dialog, which) -> {
                    String ip = etIp.getText().toString().trim();
                    String pin = etPin.getText().toString().trim();
                    if (ip.isEmpty() || pin.length() < 6) {
                        Toast.makeText(this, "Please enter a valid IP address and 6-digit PIN", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(this, "Connecting to " + ip + "...", Toast.LENGTH_SHORT).show();
                    syncEngine.pairWithDevice(ip, SyncPacket.DEFAULT_TCP_PORT, pin, (success, message) -> {
                        Toast.makeText(PairDevicesActivity.this, message, Toast.LENGTH_LONG).show();
                        updateUiState();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUiState() {
        tvThisDeviceName.setText(ledgerRepository.getOwnDeviceName());
        String localIp = getLocalIpAddress();
        tvThisDeviceIp.setText(localIp != null ? localIp : "Not connected to Wi-Fi");
        tvOwnPairingPin.setText(ledgerRepository.getOwnPairingPin());

        boolean isPaired = ledgerRepository.isPaired();
        if (isPaired) {
            layoutPairedInfo.setVisibility(View.VISIBLE);
            layoutUnpairedInfo.setVisibility(View.GONE);

            tvPairedDeviceName.setText(ledgerRepository.getPairedDeviceName());
            tvPairedDeviceIp.setText("IP: " + (ledgerRepository.getLastKnownIp().isEmpty() ? "Searching Wi-Fi..." : ledgerRepository.getLastKnownIp()));

            long lastSync = ledgerRepository.getLastSyncTimestamp();
            if (lastSync > 0) {
                tvLastSyncTime.setText("Last Synced: " + dateFormat.format(new Date(lastSync)));
            } else {
                tvLastSyncTime.setText("Last Synced: Waiting for first sync");
            }
        } else {
            layoutPairedInfo.setVisibility(View.GONE);
            layoutUnpairedInfo.setVisibility(View.VISIBLE);
        }

        // Stats
        tvSentCount.setText(String.valueOf(ledgerRepository.getOutgoingSyncedCount()));
        tvReceivedCount.setText(String.valueOf(ledgerRepository.getIncomingReceivedCount()));

        // Logs
        List<SyncLedgerRepository.SyncLogItem> logs = ledgerRepository.getSyncLogs();
        if (logs.isEmpty()) {
            tvEmptyLogs.setVisibility(View.VISIBLE);
            rvSyncLogs.setVisibility(View.GONE);
        } else {
            tvEmptyLogs.setVisibility(View.GONE);
            rvSyncLogs.setVisibility(View.VISIBLE);
            logAdapter.setLogs(logs);
        }
    }

    private final SyncEngine.SyncEngineListener syncEngineListener = new SyncEngine.SyncEngineListener() {
        @Override
        public void onStateChanged(SyncEngine.SyncState state, String detailMessage) {
            runOnUiThread(() -> {
                tvSyncDetail.setText(detailMessage);
                switch (state) {
                    case SYNCING:
                        tvSyncStatusBadge.setText("SYNCING PHOTOS");
                        tvSyncStatusBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.primary_saffron));
                        progressBarSync.setVisibility(View.VISIBLE);
                        tvProgressFile.setVisibility(View.VISIBLE);
                        break;
                    case SYNC_SUCCESS:
                        tvSyncStatusBadge.setText("SYNC COMPLETE");
                        tvSyncStatusBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_success));
                        progressBarSync.setVisibility(View.GONE);
                        tvProgressFile.setVisibility(View.GONE);
                        updateUiState();
                        break;
                    case PEER_FOUND:
                        tvSyncStatusBadge.setText("PAIRED & CONNECTED");
                        tvSyncStatusBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_success));
                        progressBarSync.setVisibility(View.GONE);
                        tvProgressFile.setVisibility(View.GONE);
                        break;
                    case SEARCHING_PEER:
                        tvSyncStatusBadge.setText("SEARCHING WI-FI...");
                        tvSyncStatusBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_warning));
                        progressBarSync.setVisibility(View.GONE);
                        tvProgressFile.setVisibility(View.GONE);
                        break;
                    case UNPAIRED:
                        tvSyncStatusBadge.setText("NOT PAIRED");
                        tvSyncStatusBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.maroon_kumkum));
                        progressBarSync.setVisibility(View.GONE);
                        tvProgressFile.setVisibility(View.GONE);
                        break;
                    case DISCONNECTED_WIFI:
                        tvSyncStatusBadge.setText("WI-FI OFF");
                        tvSyncStatusBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_error));
                        progressBarSync.setVisibility(View.GONE);
                        tvProgressFile.setVisibility(View.GONE);
                        break;
                    case SYNC_ERROR:
                        tvSyncStatusBadge.setText("SYNC ISSUE");
                        tvSyncStatusBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_error));
                        progressBarSync.setVisibility(View.GONE);
                        tvProgressFile.setVisibility(View.GONE);
                        break;
                }
            });
        }

        @Override
        public void onProgress(String currentFile, int current, int total) {
            runOnUiThread(() -> {
                progressBarSync.setVisibility(View.VISIBLE);
                progressBarSync.setIndeterminate(false);
                progressBarSync.setMax(total);
                progressBarSync.setProgress(current);
                tvProgressFile.setVisibility(View.VISIBLE);
                tvProgressFile.setText(String.format(Locale.US, "(%d/%d) %s", current, total, currentFile));
            });
        }
    };

    private String getLocalIpAddress() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo winfo = wm.getConnectionInfo();
                int ipAddress = winfo.getIpAddress();
                if (ipAddress != 0) {
                    return String.format(Locale.US, "%d.%d.%d.%d",
                            (ipAddress & 0xff),
                            (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff));
                }
            }

            // Fallback via NetworkInterface
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private class SyncLogAdapter extends RecyclerView.Adapter<SyncLogViewHolder> {
        private List<SyncLedgerRepository.SyncLogItem> items = Collections.emptyList();

        public void setLogs(List<SyncLedgerRepository.SyncLogItem> list) {
            this.items = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SyncLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sync_log, parent, false);
            return new SyncLogViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SyncLogViewHolder holder, int position) {
            SyncLedgerRepository.SyncLogItem item = items.get(position);
            holder.tvFileName.setText(item.fileName);
            holder.tvDetails.setText(item.details);
            holder.tvTime.setText(timeFormat.format(new Date(item.timestamp)));

            if ("RECEIVED".equalsIgnoreCase(item.direction)) {
                holder.tvBadge.setText("↓");
                holder.tvBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.gold_light));
                holder.tvBadge.setTextColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_success));
            } else {
                holder.tvBadge.setText("↑");
                holder.tvBadge.setBackgroundColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.primary_saffron_light));
                holder.tvBadge.setTextColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.primary_saffron_dark));
            }

            if ("SUCCESS".equalsIgnoreCase(item.status)) {
                holder.tvStatus.setText("SUCCESS");
                holder.tvStatus.setTextColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_success));
            } else {
                holder.tvStatus.setText("FAILED");
                holder.tvStatus.setTextColor(ContextCompat.getColor(PairDevicesActivity.this, R.color.status_error));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class SyncLogViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge;
        TextView tvFileName;
        TextView tvDetails;
        TextView tvStatus;
        TextView tvTime;

        public SyncLogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tvDirectionBadge);
            tvFileName = itemView.findViewById(R.id.tvLogFileName);
            tvDetails = itemView.findViewById(R.id.tvLogDetails);
            tvStatus = itemView.findViewById(R.id.tvLogStatus);
            tvTime = itemView.findViewById(R.id.tvLogTime);
        }
    }
}
