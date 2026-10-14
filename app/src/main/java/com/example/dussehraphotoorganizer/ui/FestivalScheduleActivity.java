package com.example.dussehraphotoorganizer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dussehraphotoorganizer.R;
import com.example.dussehraphotoorganizer.model.FestivalDateItem;
import com.example.dussehraphotoorganizer.schedule.AlankaramSchedule;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FestivalScheduleActivity extends AppCompatActivity {

    private RecyclerView rvFestivalSchedule;
    private AlankaramSchedule scheduleManager;
    private List<FestivalDateItem> festivalList;
    private ScheduleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_festival_schedule);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSchedule);
        toolbar.setNavigationOnClickListener(v -> finish());

        scheduleManager = new AlankaramSchedule(this);
        festivalList = scheduleManager.getSchedule();

        rvFestivalSchedule = findViewById(R.id.rvFestivalSchedule);
        rvFestivalSchedule.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleAdapter();
        rvFestivalSchedule.setAdapter(adapter);
    }

    private class ScheduleAdapter extends RecyclerView.Adapter<ScheduleViewHolder> {

        @NonNull
        @Override
        public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_festival_schedule, parent, false);
            return new ScheduleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
            FestivalDateItem item = festivalList.get(position);
            holder.tvDate.setText(item.getDate());
            holder.tvDayTithi.setText(item.getDayTithi());
            holder.tvTeluguName.setText(item.getTeluguName());
            holder.tvEnglishName.setText(item.getEnglishName());
            holder.tvFolderName.setText("Folder: " + item.getFolderName());
            holder.tvPhotoCount.setText(item.getPhotoCount() + " photos");

            holder.btnViewFolderPhotos.setOnClickListener(v -> {
                Intent intent = new Intent(FestivalScheduleActivity.this, GalleryActivity.class);
                intent.putExtra("FILTER_FOLDER", item.getFolderName());
                startActivity(intent);
            });

            holder.btnEditSchedule.setOnClickListener(v -> showEditDialog(item, position));
        }

        @Override
        public int getItemCount() {
            return festivalList.size();
        }
    }

    private static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvDayTithi;
        TextView tvTeluguName;
        TextView tvEnglishName;
        TextView tvFolderName;
        TextView tvPhotoCount;
        MaterialButton btnViewFolderPhotos;
        MaterialButton btnEditSchedule;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayTithi = itemView.findViewById(R.id.tvDayTithi);
            tvTeluguName = itemView.findViewById(R.id.tvTeluguName);
            tvEnglishName = itemView.findViewById(R.id.tvEnglishName);
            tvFolderName = itemView.findViewById(R.id.tvFolderName);
            tvPhotoCount = itemView.findViewById(R.id.tvPhotoCount);
            btnViewFolderPhotos = itemView.findViewById(R.id.btnViewFolderPhotos);
            btnEditSchedule = itemView.findViewById(R.id.btnEditSchedule);
        }
    }

    private void showEditDialog(FestivalDateItem item, int position) {
        // Clean AlertDialog with fields for date, Telugu name, and English name
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etDate = new EditText(this);
        etDate.setHint("Date (dd-MM-yyyy)");
        etDate.setText(item.getDate());
        layout.addView(etDate);

        final EditText etTelugu = new EditText(this);
        etTelugu.setHint("Telugu Devi Name");
        etTelugu.setText(item.getTeluguName());
        layout.addView(etTelugu);

        final EditText etEnglish = new EditText(this);
        etEnglish.setHint("English Devi Name");
        etEnglish.setText(item.getEnglishName());
        layout.addView(etEnglish);

        final EditText etFolder = new EditText(this);
        etFolder.setHint("Folder Name (without date)");
        etFolder.setText(item.getFolderName());
        layout.addView(etFolder);

        new AlertDialog.Builder(this)
                .setTitle("Edit Festival Entry")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    item.setDate(etDate.getText().toString().trim());
                    item.setTeluguName(etTelugu.getText().toString().trim());
                    item.setEnglishName(etEnglish.getText().toString().trim());
                    String cleanFolder = etFolder.getText().toString().trim().replaceAll("[^a-zA-Z0-9_]", "_");
                    item.setFolderName(cleanFolder);

                    festivalList.set(position, item);
                    scheduleManager.saveSchedule(festivalList);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "Schedule updated locally.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
