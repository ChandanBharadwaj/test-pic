import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class TokenService {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final String scope;

    public TokenService(
            WebClient.Builder webClientBuilder,
            @Value("${azure.client-id}") String clientId,
            @Value("${azure.client-secret}") String clientSecret,
            @Value("${azure.token-url}") String tokenUrl,
            @Value("${azure.scope}") String scope) {
        this.webClient = webClientBuilder.baseUrl(tokenUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.scope = scope;
    }

    public String getAccessToken() {
        Map<String, String> tokenResponse = webClient.post()
                .uri("")
                .bodyValue(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "grant_type", "client_credentials",
                        "scope", scope
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
            return tokenResponse.get("access_token").toString();
        } else {
            throw new RuntimeException("Failed to retrieve access token");
        }
    }
}
