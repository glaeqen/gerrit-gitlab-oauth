import com.google.gson.GsonBuilder;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;

public class Playground {
    // FIXME: Mock gitlab4j and write some proper UTs maybe.
    public static void main(String[] args) throws GitLabApiException {
        var oauth_gl = new GitLabApi("https://gitlab.com", Constants.TokenType.OAUTH2_ACCESS, "<GET_NEW_TOKEN>");
        var gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        var authed_user_id = oauth_gl.getUserApi().getCurrentUser().getId();
    }
}
