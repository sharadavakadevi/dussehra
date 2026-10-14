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
import java.util.Map;

public class ManageLocationsActivity extends AppCompatActivity {

    private RecyclerView rvLocations;
    private MaterialButton btnTestCoordinates;
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
        btnTestCoordinates = findViewById(R.id.btnTestCoordinates);
        btnResetLocations = findViewById(R.id.btnResetLocations);

        configManager = new LocationConfigManager(this);

        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter();
        rvLocations.setAdapter(adapter);

        btnTestCoordinates.setOnClickListener(v -> showTestCoordinatesDialog());

        btnResetLocations.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reset to Defaults")
                    .setMessage("Reset all 3 locations (Home, Yeendda Durga Devi Temple, Carshed Bobleam) to user-verified coordinates & 40m radius?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        configManager.resetToDefaults();
                        loadLocations();
                        Toast.makeText(this, "Locations reset to default verified settings.", Toast.LENGTH_SHORT).show();
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

    /**
     * Test function showing photo coordinates, distances to all three locations,
     * selected alias, and full destination path under Pictures/Dussehra_2026/[Devi]/[Location_Or_Date]/.
     */
    private void showTestCoordinatesDialog() {
        String[] options = new String[]{
                "📍 Test Home Pin (17.782889, 83.361833)",
                "📍 Test Temple Pin (17.781944, 83.361917)",
                "📍 Test Carshed Pin (17.802778, 83.352667)",
                "🌊 Test Outside Pin / RK Beach (17.712500, 83.321000)",
                "⚠️ Test Ambiguous Midpoint (Overlapping Radius)",
                "📅 Test Photo with No GPS Data",
                "✏️ Enter Custom Coordinates..."
        };

        new AlertDialog.Builder(this)
                .setTitle("Select Test Location Case")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            runLocationTest(17.782889, 83.361833, "12-10-2026", "2026-10-12");
                            break;
                        case 1:
                            runLocationTest(17.781944, 83.361917, "13-10-2026", "2026-10-13");
                            break;
                        case 2:
                            runLocationTest(17.802778, 83.352667, "14-10-2026", "2026-10-14");
                            break;
                        case 3:
                            runLocationTest(17.712500, 83.321000, "15-10-2026", "2026-10-15");
                            break;
                        case 4:
                            // Midpoint between Home and Temple (~53m from each)
                            runLocationTest(17.782416, 83.361875, "16-10-2026", "2026-10-16");
                            break;
                        case 5:
                            runLocationTest(null, null, "17-10-2026", "2026-10-17");
                            break;
                        case 6:
                            showCustomCoordinateInputDialog();
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCustomCoordinateInputDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etLat = new EditText(this);
        etLat.setHint("Latitude (e.g. 17.782889)");
        layout.addView(etLat);

        EditText etLon = new EditText(this);
        etLon.setHint("Longitude (e.g. 83.361833)");
        layout.addView(etLon);

        EditText etDate = new EditText(this);
        etDate.setHint("Capture Date (dd-MM-yyyy e.g. 12-10-2026)");
        etDate.setText("12-10-2026");
        layout.addView(etDate);

        new AlertDialog.Builder(this)
                .setTitle("Enter Custom Coordinates")
                .setView(layout)
                .setPositiveButton("Evaluate", (dialog, which) -> {
                    try {
                        String latStr = etLat.getText().toString().trim();
                        String lonStr = etLon.getText().toString().trim();
                        Double lat = latStr.isEmpty() ? null : Double.parseDouble(latStr);
                        Double lon = lonStr.isEmpty() ? null : Double.parseDouble(lonStr);

                        String dateStr = etDate.getText().toString().trim();
                        String isoDate = "2026-10-12";
                        if (dateStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
                            String[] parts = dateStr.split("-");
                            isoDate = parts[2] + "-" + parts[1] + "-" + parts[0];
                        }

                        runLocationTest(lat, lon, dateStr, isoDate);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid coordinates format.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runLocationTest(Double lat, Double lon, String dateFormatted, String isoDate) {
        LocationConfigManager.EvaluationResult result = configManager.evaluatePhotoDestination(
                lat, lon, dateFormatted, isoDate
        );

        StringBuilder msg = new StringBuilder();
        msg.append("1. Photo EXIF Coordinates:\n   ");
        if (lat != null && lon != null) {
            msg.append(String.format(Locale.US, "%.6f° N, %.6f° E", lat, lon));
        } else {
            msg.append("(No GPS metadata present)");
        }
        msg.append("\n\n");

        msg.append("2. Distances to Registered Locations:\n");
        if (!result.distancesToLocations.isEmpty()) {
            for (LocationConfigItem item : locationList) {
                Float dist = result.distancesToLocations.get(item.getFolderName());
                if (dist != null) {
                    boolean inside = dist <= item.getRadiusMeters();
                    msg.append(String.format(Locale.US, "   • %s: %.1f m (Radius: %.0f m) -> %s\n",
                            item.getName(), dist, item.getRadiusMeters(), inside ? "MATCH ✓" : "Outside ✗"));
                }
            }
        } else {
            msg.append("   • (GPS missing - distances cannot be calculated)\n");
        }
        msg.append("\n");

        msg.append("3. Match Decision:\n   • ");
        if ("SINGLE_MATCH".equals(result.matchType)) {
            msg.append("Selected Alias: ").append(result.selectedAlias).append(" (Exact single match)");
        } else if ("AMBIGUOUS_MULTIPLE_MATCH".equals(result.matchType)) {
            msg.append("Ambiguous: Multiple locations matched! Fallback to date folder: ").append(result.locationSubfolder);
        } else if ("UNMATCHED_GPS".equals(result.matchType)) {
            msg.append("Unmatched: No locations within radius. Fallback to date folder: ").append(result.locationSubfolder);
        } else {
            msg.append("No GPS metadata: Fallback to date folder: ").append(result.locationSubfolder);
        }
        msg.append("\n\n");

        msg.append("4. Devi Parent Folder:\n   • ")
                .append(result.deviParentFolder)
                .append(" (Based on festival date: ").append(dateFormatted).append(")\n\n");

        msg.append("5. Final Destination Path:\n   • Pictures/")
                .append(result.destinationRelativePath)
                .append("/");

        new AlertDialog.Builder(this)
                .setTitle("GPS Matching Test Result")
                .setMessage(msg.toString())
                .setPositiveButton("OK", null)
                .show();
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
        etLat.setHint("Latitude (e.g. 17.782889)");
        etLat.setText(String.format(Locale.US, "%.6f", item.getLatitude()));
        layout.addView(etLat);

        EditText etLon = new EditText(this);
        etLon.setHint("Longitude (e.g. 83.361833)");
        etLon.setText(String.format(Locale.US, "%.6f", item.getLongitude()));
        layout.addView(etLon);

        EditText etRadius = new EditText(this);
        etRadius.setHint("Radius in meters (e.g. 40)");
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
            holder.tvLocationFolder.setText("Folder Alias: " + item.getFolderName());
            holder.tvLocationTelugu.setText(item.getTeluguName());
            holder.tvLocationAddress.setText(item.getAddress());
            holder.tvLocationCoordinates.setText(String.format(Locale.US,
                    "GPS: %.6f° N, %.6f° E\nRadius: %.0f meters (Non-overlapping)",
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
