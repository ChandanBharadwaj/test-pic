import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.Collections;

@Service
public class TokenService {

    private final String clientId;
    private final String clientSecret;
    private final String authority;
    private final String scope;

    public TokenService(
            @Value("${azure.client-id}") String clientId,
            @Value("${azure.client-secret}") String clientSecret,
            @Value("${azure.authority}") String authority,
            @Value("${azure.scope}") String scope) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authority = authority;
        this.scope = scope;
    }

    public String getAccessToken() {
        try {
            // Create a ConfidentialClientApplication instance
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId,
                            com.microsoft.aad.msal4j.ClientSecret.create(clientSecret))
                    .authority(authority)
                    .build();

            // Build parameters for acquiring a token
            ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                            Collections.singleton(scope))
                    .build();

            // Acquire the token
            IAuthenticationResult result = app.acquireToken(parameters).join();

            // Return the access token
            return result.accessToken();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Authority URL", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to acquire token", e);
        }
    }
}
