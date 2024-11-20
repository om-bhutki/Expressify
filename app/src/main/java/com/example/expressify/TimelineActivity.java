package com.example.expressify;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TimelineActivity extends AppCompatActivity {

    private LineChart timelineChart;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        timelineChart = findViewById(R.id.timelineChart);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Fetch and display data
        fetchWeeklyHappinessData();
    }

    private void fetchWeeklyHappinessData() {
        // Initialize dates for the last 7 days
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        ArrayList<String> lastSevenDays = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            Calendar tempCalendar = (Calendar) calendar.clone(); // Clone the calendar to avoid cumulative modification
            tempCalendar.add(Calendar.DATE, -i);
            lastSevenDays.add(sdf.format(tempCalendar.getTime()));
        }


        // Fetch data from Firebase
        mDatabase.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Map<String, Integer> happinessCounts = new HashMap<>();

                // Initialize with 0 counts
                for (String date : lastSevenDays) {
                    happinessCounts.put(date, 0);
                }

                // Parse Firebase data
                for (String date : lastSevenDays) {
                    if (task.getResult().child(date).exists()) {
                        for (DataSnapshot snapshot : task.getResult().child(date).getChildren()) {
                            String emotion = snapshot.getValue(String.class);
                            if ("Happy".equalsIgnoreCase(emotion)) {
                                happinessCounts.put(date, happinessCounts.get(date) + 1);
                            }
                        }
                    }
                }

                populateTimelineChart(lastSevenDays, happinessCounts);
            } else {
                Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                Log.e("FirebaseError", "Error: " + task.getException().getMessage());
            }
        });
    }

    private void populateTimelineChart(ArrayList<String> dates, Map<String, Integer> happinessCounts) {
        ArrayList<Entry> entries = new ArrayList<>();
        int index = 0;

        // Prepare SimpleDateFormat to get the abbreviated day of the week
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault()); // "EEE" is for abbreviated day (Mon, Tue, etc.)

        for (String date : dates) {
            // Add entry for the chart
            entries.add(new Entry(index, happinessCounts.get(date)));
            index++;
        }

        LineDataSet lineDataSet = new LineDataSet(entries, "Weekly Happiness");
        lineDataSet.setColor(getResources().getColor(R.color.gradient));
        lineDataSet.setCircleColor(getResources().getColor(R.color.navy));
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setValueTextSize(12f);

        LineData lineData = new LineData(lineDataSet);

        // Configure chart appearance
        timelineChart.setData(lineData);
        timelineChart.getDescription().setEnabled(false);

        // Configure X-axis
        XAxis xAxis = timelineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = Math.round(value); // Round the float value to the nearest integer
                if (idx >= 0 && idx < dates.size()) {
                    try {
                        // Convert date to a Day of the Week (short form)
                        Date parsedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates.get(idx));
                        return parsedDate != null ? sdf.format(parsedDate) : ""; // Return the day abbreviation (Mon, Tue, etc.)
                    } catch (ParseException e) {
                        Log.d("Date", "Error parsing date: " + dates.get(idx));

                    }
                }
                return ""; // Return empty if the index is out of bounds
            }
        });
        xAxis.setDrawGridLines(false); // Remove X-axis gridlines

        // Configure Y-axis
        YAxis leftAxis = timelineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false); // Remove Y-axis gridlines
        timelineChart.getAxisRight().setEnabled(false);

        // Refresh chart
        timelineChart.invalidate();
    }

}
