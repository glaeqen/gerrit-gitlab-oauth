package eu.glaeqen.gerritgitlaboauth;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.auth.oauth.OAuthLoginProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class Module extends AbstractModule {
    private final String pluginName;

    @Inject
    Module(@PluginName String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    protected void configure() {
        bind(OAuthLoginProvider.class)
                .annotatedWith(Exports.named(pluginName))
                .to(DisabledOAuthLoginProvider.class);
    }
}
