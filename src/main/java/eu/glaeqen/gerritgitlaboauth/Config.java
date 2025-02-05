package eu.glaeqen.gerritgitlaboauth;

import com.google.common.base.CharMatcher;
import com.google.common.primitives.UnsignedInteger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Singleton
class Config {
    private static final String OAUTH_CLIENT_ID_CONFIG_KEY = "oauth-client-id";
    private static final String OAUTH_CLIENT_SECRET_CONFIG_KEY = "oauth-client-secret";
    private static final String ORG_PRIVATE_TOKEN_KEY = "org-private-token";
    private static final String PREFERRED_EMAIL_MATCHER = "preferred-email-matcher";
    private static final String PROJECT_MEMBERSHIP_KEY = "project-membership";
    private static final String GROUP_MEMBERSHIP_KEY = "group-membership";
    private static final String DEFAULT_ROOT_URL_VALUE = "https://gitlab.com";

    private final PluginConfig cfg;
    private final String oauthCallbackUrl;
    private final String oauthClientId;
    private final String oauthClientSecret;
    private final Pattern[] preferredEmailMatcher;
    private final Optional<String> organizationPrivateToken;
    private final Optional<UnsignedInteger> projectMembership;
    private final Optional<UnsignedInteger> groupMembership;

    @Inject
    public Config(
            @PluginName String pluginName,
            PluginConfigFactory cfgFactory,
            @CanonicalWebUrl Provider<String> urlProvider
    ) {
        cfg = cfgFactory.getFromGerritConfig(pluginName);
        oauthCallbackUrl = String.format("%s/oauth", CharMatcher.is('/').trimTrailingFrom(urlProvider.get()));
        oauthClientId = getRequired(Config.OAUTH_CLIENT_ID_CONFIG_KEY);
        oauthClientSecret = getRequired(Config.OAUTH_CLIENT_SECRET_CONFIG_KEY);
        organizationPrivateToken = getOptional(Config.ORG_PRIVATE_TOKEN_KEY);
        preferredEmailMatcher = getOptionalRegexList(Config.PREFERRED_EMAIL_MATCHER);
        projectMembership = getOptional(Config.PROJECT_MEMBERSHIP_KEY)
                .map(v -> toUnsignedInteger(v, "Project ID must be an unsigned integer"));
        groupMembership = getOptional(Config.GROUP_MEMBERSHIP_KEY)
                .map(v -> toUnsignedInteger(v, "Group ID must be an unsigned integer"));
    }

    public String getOauthCallbackUrl() {
        return oauthCallbackUrl;
    }

    public String getOauthClientId() {
        return oauthClientId;
    }

    public String getOauthClientSecret() {
        return oauthClientSecret;
    }

    public Pattern[] getPreferredEmailMatcher() {
        return preferredEmailMatcher;
    }

    public Optional<String> getOrganizationPrivateToken() {
        return organizationPrivateToken;
    }

    public Optional<UnsignedInteger> getProjectMembership() {
        return projectMembership;
    }

    public Optional<UnsignedInteger> getGroupMembership() {
        return groupMembership;
    }

    public String getRootUrl() {
        return DEFAULT_ROOT_URL_VALUE;
    }

    private String getRequired(String key) {
        return Objects.requireNonNull(cfg.getString(key), String.format("'%s' is not set in the plugin configuration", key));
    }

    private String[] getOptionalList(String key) {
        var list = cfg.getStringList(key);
        if (list != null) {
            return list;
        }
        return new String[0];
    }

    private Pattern[] getOptionalRegexList(String key) {
        return Arrays.stream(getOptionalList(key)).map(Pattern::compile).toArray(Pattern[]::new);
    }

    private Optional<String> getOptional(String key) {
        return Optional.ofNullable(cfg.getString(key));
    }

    private static UnsignedInteger toUnsignedInteger(String from, String exceptionMessage) {
        try {
            return UnsignedInteger.valueOf(from);
        } catch (NumberFormatException ignored) {
            throw new RuntimeException(exceptionMessage);
        }
    }
}
