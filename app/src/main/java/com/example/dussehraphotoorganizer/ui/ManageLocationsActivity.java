package com.example.dussehraphotoorganizer.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.location.LocationConfigManager;
import com.example.dussehraphotoorganizer.model.LocationConfigItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageLocationsActivity extends AppCompatActivity {

    private RecyclerView rvLocations;
    private MaterialButton btnResetLocations;
    private LocationConfigManager configManager;
    private final List<LocationConfigItem> locationList = new ArrayList<>();
    private LocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_locations);

        MaterialToolbar toolbar = findViewById(R.id.toolbarLocations);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvLocations = findViewById(R.id.rvLocations);
        btnResetLocations = findViewById(R.id.btnResetLocations);

        configManager = new LocationConfigManager(this);

        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter();
        rvLocations.setAdapter(adapter);

        btnResetLocations.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reset to Defaults")
                    .setMessage("Reset all 3 locations (Home, Yeendda Durga Devi Temple, Carshed Bobleam) to initial coordinates & radius?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        configManager.resetToDefaults();
                        loadLocations();
                        Toast.makeText(this, "Locations reset to default settings.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        loadLocations();
    }

    private void loadLocations() {
        locationList.clear();
        locationList.addAll(configManager.getRegisteredLocations());
        adapter.notifyDataSetChanged();
    }

    private void showEditDialog(LocationConfigItem item, int position) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(item.getAddress());
        tvInfo.setTextSize(12);
        tvInfo.setPadding(0, 0, 0, 16);
        layout.addView(tvInfo);

        EditText etLat = new EditText(this);
        etLat.setHint("Latitude (e.g. 17.7772)");
        etLat.setText(String.format(Locale.US, "%.6f", item.getLatitude()));
        layout.addView(etLat);

        EditText etLon = new EditText(this);
        etLon.setHint("Longitude (e.g. 83.3616)");
        etLon.setText(String.format(Locale.US, "%.6f", item.getLongitude()));
        layout.addView(etLon);

        EditText etRadius = new EditText(this);
        etRadius.setHint("Radius in meters (e.g. 250)");
        etRadius.setText(String.format(Locale.US, "%.0f", item.getRadiusMeters()));
        layout.addView(etRadius);

        new AlertDialog.Builder(this)
                .setTitle("Edit " + item.getName())
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double lat = Double.parseDouble(etLat.getText().toString().trim());
                        double lon = Double.parseDouble(etLon.getText().toString().trim());
                        float radius = Float.parseFloat(etRadius.getText().toString().trim());

                        item.setLatitude(lat);
                        item.setLongitude(lon);
                        item.setRadiusMeters(radius);

                        configManager.saveLocations(locationList);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "Location saved.", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid number format entered.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class LocationAdapter extends RecyclerView.Adapter<LocationViewHolder> {

        @NonNull
        @Override
        public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_config, parent, false);
            return new LocationViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
            LocationConfigItem item = locationList.get(position);
            holder.tvLocationName.setText(item.getName());
            holder.tvLocationFolder.setText("Folder: " + item.getFolderName());
            holder.tvLocationTelugu.setText(item.getTeluguName());
            holder.tvLocationAddress.setText(item.getAddress());
            holder.tvLocationCoordinates.setText(String.format(Locale.US,
                    "GPS: %.4f° N, %.4f° E\nRadius: %.0f meters",
                    item.getLatitude(), item.getLongitude(), item.getRadiusMeters()));

            holder.btnEditLocation.setOnClickListener(v -> showEditDialog(item, position));
        }

        @Override
        public int getItemCount() {
            return locationList.size();
        }
    }

    private static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName;
        TextView tvLocationFolder;
        TextView tvLocationTelugu;
        TextView tvLocationAddress;
        TextView tvLocationCoordinates;
        MaterialButton btnEditLocation;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvLocationFolder = itemView.findViewById(R.id.tvLocationFolder);
            tvLocationTelugu = itemView.findViewById(R.id.tvLocationTelugu);
            tvLocationAddress = itemView.findViewById(R.id.tvLocationAddress);
            tvLocationCoordinates = itemView.findViewById(R.id.tvLocationCoordinates);
            btnEditLocation = itemView.findViewById(R.id.btnEditLocation);
        }
    }
}
