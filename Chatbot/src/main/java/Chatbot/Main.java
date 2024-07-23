package Chatbot;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main extends Application {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String OPENAI_API_KEY = dotenv.get("OPENAI_API_KEY");
    private static final int MAX_INPUT_LENGTH = 100; // Limit input to 100 characters
    private static final int MAX_REQUESTS_PER_MINUTE = 5; // Limit to 5 requests per minute

    @Override
    public void start(Stage primaryStage) {
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);

        TextField userInput = new TextField();
        Button sendButton = new Button("Send");

        sendButton.setOnAction(e -> {
            String userText = userInput.getText();
            if (userText.length() > MAX_INPUT_LENGTH) {
                chatArea.appendText("Bot: Input too long. Please limit your input to " + MAX_INPUT_LENGTH + " characters.\n");
                userInput.clear();
                return;
            }

            chatArea.appendText("User: " + userText + "\n");
            try {
                String response = getAIResponse(userText);
                chatArea.appendText("Bot: " + response + "\n");
            } catch (Exception ex) {
                chatArea.appendText("Bot: error\n");
                ex.printStackTrace();
            }
            userInput.clear();
        });

        VBox root = new VBox(10, chatArea, userInput, sendButton);
        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("Chatbot");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String getAIResponse(String input) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://api.openai.com/v1/chat/completions");
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + OPENAI_API_KEY);

        String json = "{"
                + "\"model\": \"gpt-4o-mini\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"" + input + "\"}],"
                + "\"max_tokens\": 50"
                + "}";
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(httpPost);
        String responseBody = EntityUtils.toString(response.getEntity());
        response.close();

        // Print the response for debugging
        System.out.println("Response: " + responseBody);

        // Extract the response text from the JSON response
        String responseText = extractResponseText(responseBody);
        return responseText;
    }

    private String extractResponseText(String responseBody) {
        JSONObject jsonObject = new JSONObject(responseBody);
        JSONArray choices = jsonObject.getJSONArray("choices");
        if (choices.length() > 0) {
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            return message.getString("content").trim();
        }
        return "error";
    }

    public static void main(String[] args) {
        launch(args);
    }
}