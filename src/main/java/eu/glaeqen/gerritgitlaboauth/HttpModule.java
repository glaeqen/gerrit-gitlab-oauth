package eu.glaeqen.gerritgitlaboauth;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.auth.oauth.OAuthServiceProvider;
import com.google.inject.Inject;
import com.google.inject.servlet.ServletModule;

class HttpModule extends ServletModule {
    private final String pluginName;

    @Inject
    HttpModule(@PluginName String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    protected void configureServlets() {
        bind(OAuthServiceProvider.class)
                .annotatedWith(Exports.named(pluginName))
                .to(GitLabOAuthService.class);
        bind(Config.class);
    }
}