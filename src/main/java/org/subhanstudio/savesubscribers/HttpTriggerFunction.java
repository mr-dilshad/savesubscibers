package org.subhanstudio.savesubscribers;


import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerFunction {
    private static final String STORAGE_CONNECTION_STRING = System.getenv("AzureWebJobsStorage");

    @FunctionName("subscribersave")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing request to save subscriber email.");

        // Parse the request body to get the email
        String requestBody = request.getBody().orElse("");
        if (!requestBody.contains("email")) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid request. Please provide a valid email.")
                    .build();
        }

        String email = requestBody.split(":")[1].replace("\"", "").replace("}", "").trim();

        // Save the email in Azure Table Storage
        TableClient tableClient = new TableClientBuilder()
                .connectionString(STORAGE_CONNECTION_STRING)
                .tableName("Subscribers")
                .buildClient();

        TableEntity subscriberEntity = new TableEntity("Subscriber", email)
                .addProperty("email", email)
                .addProperty("timestamp", System.currentTimeMillis());

        try {
            tableClient.createEntity(subscriberEntity);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Email saved successfully.")
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Error saving email: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving subscriber email.")
                    .build();
        }
    }
}
