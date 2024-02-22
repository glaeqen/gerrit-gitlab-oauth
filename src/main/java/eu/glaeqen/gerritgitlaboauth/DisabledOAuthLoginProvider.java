package eu.glaeqen.gerritgitlaboauth;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.auth.oauth.OAuthLoginProvider;
import com.google.gerrit.extensions.auth.oauth.OAuthUserInfo;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;

@Singleton
class DisabledOAuthLoginProvider implements OAuthLoginProvider {
    private final String pluginName;

    @Inject
    DisabledOAuthLoginProvider(@PluginName String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public OAuthUserInfo login(String username, String secret) {
        throw new UnsupportedOperationException(String.format("git over OAuth is not implemented by %s", pluginName));
    }
}
