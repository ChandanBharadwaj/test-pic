import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class SharePointService {

    private final WebClient webClient;
    private final AccessTokenGenerator accessTokenGenerator;

    public SharePointService(WebClient webClient, AccessTokenGenerator accessTokenGenerator) {
        this.webClient = webClient;
        this.accessTokenGenerator = accessTokenGenerator;
    }

    public Mono<String> getAllFiles(String siteId, String driveId, String folderId) {
        String accessToken = accessTokenGenerator.getAccessToken();
        String url = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/%s/children", 
                                    siteId, driveId, folderId);

        return webClient.get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    // Handle errors appropriately
                    return Mono.error(new RuntimeException("Failed to retrieve files", ex));
                });
    }
}
