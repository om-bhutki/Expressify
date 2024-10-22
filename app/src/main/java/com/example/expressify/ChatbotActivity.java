package com.example.expressify;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editTextMessage;
    private Button buttonSend;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize the views
        recyclerView = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        // Set up RecyclerView
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        // Initialize the API service
        apiService = RetrofitClient.getClient("https://chatbot-gemini-4xtz.onrender.com/").create(ApiService.class);

        // Button click listener to send message
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = editTextMessage.getText().toString();
                if (!messageText.isEmpty()) {
                    // Add user message to the list
                    messageList.add(new Message("User", messageText));
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);

                    // Send the message to the API
                    sendMessageToApi(messageText);
                    editTextMessage.setText(""); // Clear the input field
                }
            }
        });
    }

    private String processMessage(String msg) {
        // Remove all asterisks from the message
        msg = msg.replace("*", "");

        // Check if the message length is greater than 100
        if (msg.length() > 100) {
            // Find the index of the first full stop after the first 100 characters
            int firstFullStopIndex = msg.indexOf('.', 100);

            // If a full stop is found, limit the message to that point
            if (firstFullStopIndex != -1) {
                msg = msg.substring(0, firstFullStopIndex + 1); // Include the full stop
            } else {
                // If no full stop is found after 100 characters, keep the message as is
                msg = msg.substring(0, 100);
            }
        }

        return msg;
    }


    private void sendMessageToApi(String messageText) {
        // Create a UserMessage object with the user's input
        UserMessage userMessage = new UserMessage(messageText);

        // Call the API
        apiService.sendMessage(userMessage).enqueue(new Callback<BotResponse>() {
            @Override
            public void onResponse(Call<BotResponse> call, Response<BotResponse> response) {
                // Log the response details
                Log.d("API Response", "Code: " + response.code() + ", Message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    // Log the response body for debugging
                    Log.d("API Response Body", "Response: " + new Gson().toJson(response.body()));

                    // Add bot response to the list
                    String msg = processMessage(response.body().getResponse());

                    messageList.add(new Message("Bot", msg));
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                } else {
                    // Log error response
                    Log.e("API Error", "Error: " + response.message());
                    messageList.add(new Message("Bot", "Error: " + response.message()));
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<BotResponse> call, Throwable t) {
                // Log failure
                Log.e("API Failure", "Error: " + t.getMessage());
                messageList.add(new Message("Bot", "Error: " + t.getMessage()));
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        });
    }
}
