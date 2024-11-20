package com.example.expressify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private BarChart barChart;
    private PieChart pieChart;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        calendarView = findViewById(R.id.calendar_view);
        barChart = findViewById(R.id.bar_chart);
        pieChart = findViewById(R.id.pie_chart);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Format the selected date
            String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            fetchAndDisplayEmotions(selectedDate);
        });

        Button timelineBtn = findViewById(R.id.timelinebtn);
        timelineBtn.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this,TimelineActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)));

    }

    private void fetchAndDisplayEmotions(String date) {
        DatabaseReference dateRef = mDatabase.child(userId).child(date);

        dateRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Map<String, Integer> emotionCounts = new HashMap<>();

                // Count occurrences of each emotion
                for (DataSnapshot emotionSnapshot : task.getResult().getChildren()) {
                    String emotion = emotionSnapshot.getValue(String.class);
                    if (emotion != null) {
                        emotionCounts.put(emotion, emotionCounts.getOrDefault(emotion, 0) + 1);
                    }
                }

                // Create a bar chart with the data
                displayBarChart(emotionCounts);

                // Create a pie chart with the data
                displayPieChart(emotionCounts);
            } else {
                Toast.makeText(this, "No data available for the selected date", Toast.LENGTH_SHORT).show();
                barChart.clear();
                barChart.invalidate();
                pieChart.clear();
                pieChart.invalidate();
            }
        });
    }

    private void displayBarChart(Map<String, Integer> emotionCounts) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Emotion Frequency");
        dataSet.setValueTextSize(12f); // Set text size for bar values
        dataSet.setColors(new int[]{R.color.blue}, getApplicationContext()); // Set bar color

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f); // Adjust bar width

        barChart.setData(barData);
        barChart.setFitBars(true);

        // Remove grid lines and left/right axis labels
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value); // Safely round to nearest int
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index); // Return valid label
                }
                return ""; // Fallback for invalid indices
            }
        });

        barChart.getAxisLeft().setDrawLabels(false); // Remove left axis labels
        barChart.getAxisRight().setDrawLabels(false); // Remove right axis labels
        barChart.getAxisLeft().setDrawGridLines(false); // Remove left grid lines
        barChart.getAxisRight().setDrawGridLines(false); // Remove right grid lines

        // Disable chart legend and description
        barChart.getLegend().setEnabled(false);
        barChart.getDescription().setEnabled(false);

        // Show value of each bar
        barData.setValueTextSize(14f);
        barData.setValueTextColor(getResources().getColor(R.color.black, null));

        barChart.animateY(1000); // Add animation
        barChart.invalidate(); // Refresh the chart
    }

    private void displayPieChart(Map<String, Integer> emotionCounts) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Emotion Distribution");
        dataSet.setColors(new int[]{R.color.colorSecondaryLight,R.color.gradient,R.color.vintage,R.color.beachblue,R.color.navy,R.color.blue}, getApplicationContext());
        dataSet.setValueTextSize(14f);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);

        // Customize PieChart
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false); // Disable legend

        pieChart.animateY(1000); // Add animation
        pieChart.invalidate(); // Refresh the chart
    }
}
