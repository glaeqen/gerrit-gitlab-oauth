package eu.glaeqen.gerritgitlaboauth;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;
import com.google.common.primitives.UnsignedInteger;
import com.google.gerrit.extensions.auth.oauth.OAuthServiceProvider;
import com.google.gerrit.extensions.auth.oauth.OAuthToken;
import com.google.gerrit.extensions.auth.oauth.OAuthUserInfo;
import com.google.gerrit.extensions.auth.oauth.OAuthVerifier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Email;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class GitLabOAuthService implements OAuthServiceProvider {
    private static final Logger log = getLogger(GitLabOAuthService.class);
    private static final String IDENTITY_PROVIDER_PREFIX = "gitlab-oauth";
    private final OAuth20Service service;
    private final Config config;

    @Inject
    GitLabOAuthService(Config config) {
        service =
                new ServiceBuilder(config.getOauthClientId())
                        .apiSecret(config.getOauthClientSecret())
                        .callback(config.getOauthCallbackUrl())
                        .defaultScope("read_user")
                        .build(new DefaultApi20() {
                            @Override
                            public String getAccessTokenEndpoint() {
                                return String.format("%s/oauth/token", config.getRootUrl());
                            }

                            @Override
                            protected String getAuthorizationBaseUrl() {
                                return String.format("%s/oauth/authorize", config.getRootUrl());
                            }

                            @Override
                            public ClientAuthentication getClientAuthentication() {
                                return RequestBodyAuthenticationScheme.instance();
                            }
                        });
        this.config = config;
    }

    @Override
    public OAuthUserInfo getUserInfo(OAuthToken token) throws IOException {
        if (token == null) {
            log.info("No token -> no user");
            return null;
        }
        OAuthUserInfo user;
        Long userId;
        try (
                var apiViaOauth = new GitLabApi(
                        config.getRootUrl(),
                        Constants.TokenType.OAUTH2_ACCESS,
                        token.getToken())
        ) {
            var userFromApi = apiViaOauth.getUserApi().getCurrentUser();
            userId = userFromApi.getId();
            if (userId == null) {
                log.info("User does not have an id? Failing authentication.");
                return null;
            }
            var username = userFromApi.getUsername();
            if (username == null) {
                log.warn("User {} does not have a username?", userId);
                username = "UNKNOWN_USERNAME";
            }
            var mainEmail = userFromApi.getEmail();
            if (mainEmail == null) {
                log.warn("User {} does not have an email?", userId);
                mainEmail = "UNKNOWN_EMAIL";
            }
            var emailDomain = config.getEmailDomain();
            String email;
            if (emailDomain.isPresent()) {
                var validEmails = apiViaOauth.getUserApi().getEmails().stream().map(Email::getEmail).filter(e -> e.endsWith(emailDomain.get())).toList();
                var validEmailCount = validEmails.size();
                if (validEmailCount != 1) {
                    log.info("User {} has {} != 1 valid emails. Failing authentication.", userId, validEmailCount);
                    return null;
                }
                email = validEmails.get(0);
            } else {
                email = mainEmail;
            }
            var name = userFromApi.getName();
            if (name == null) {
                log.warn("User {} does not have a name?", userId);
                name = "UNKNOWN_NAME";
            }
            user = new OAuthUserInfo(
                    String.format("%s:%s", IDENTITY_PROVIDER_PREFIX, userId),
                    username,
                    email,
                    name,
                    null
            );
        } catch (GitLabApiException e) {
            throw new IOException("Could not retrieve information from the external API", e);
        }
        if (config.getOrganizationPrivateToken().isPresent()) {
            try (
                    var apiViaPrivateToken = new GitLabApi(
                            config.getRootUrl(),
                            config.getOrganizationPrivateToken().get()
                    )
            ) {
                var groupId = config.getGroupMembership().map(UnsignedInteger::longValue);
                if (groupId.isPresent()) {
                    try {
                        apiViaPrivateToken.getGroupApi().getMember(groupId.get(), userId, true);
                    } catch (GitLabApiException e) {
                        if (e.getHttpStatus() == 404) {
                            log.info("User {} is not a member of a required group. Failing authentication.", userId);
                            return null;
                        } else {
                            throw e;
                        }
                    }
                }
                var projectId = config.getProjectMembership().map(UnsignedInteger::longValue);
                if (projectId.isPresent()) {
                    try {
                        apiViaPrivateToken.getProjectApi().getMember(projectId.get(), userId, true);
                    } catch (GitLabApiException e) {
                        if (e.getHttpStatus() == 404) {
                            log.info("User {} is not a member of a required project. Failing authentication.", userId);
                            return null;
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (GitLabApiException e) {
                throw new IOException("Could not retrieve information from the external API", e);
            }
        }
        return user;
    }


    @Override
    public OAuthToken getAccessToken(OAuthVerifier rv) {
        try {
            var accessToken = service.getAccessToken(rv.getValue());
            return new OAuthToken(
                    accessToken.getAccessToken(), accessToken.getTokenType(), accessToken.getRawResponse());
        } catch (OAuth2AccessTokenErrorResponse e) {
            // This can happen when hitting refresh on Unauthorized page.
            // It keeps Gerrit to report an unauthorized page, instead of server error.
            log.warn("Could not request a new access token: {}", e.toString());
            return null;
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException("Cannot retrieve access token", e);
        }
    }

    @Override
    public String getAuthorizationUrl() {
        return service.getAuthorizationUrl();
    }

    @Override
    public String getVersion() {
        return service.getVersion();
    }

    @Override
    public String getName() {
        return "GitLab OAuth2";
    }
}
