package eu.glaeqen.gerritgitlaboauth;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class GitLabOAuthServiceTest {
    @Test
    void verifyEmailGetting() {
        var preferredEmailMatcher = new Pattern[]{
                Pattern.compile("^[^@]+@match.com$"),
                Pattern.compile("^[^@]+@alt2.eu$"),
                Pattern.compile("^[^@]+@alt.com$"),
        };
        var mainEmail = "womp womp";

        // Should match `match.com`
        var email = GitLabOAuthService.getEmail(preferredEmailMatcher, mainEmail, new String[]{
                "test1@alt.com",
                "test2@alt2.eu",
                "test3@match.com"
        });
        assertEquals("test3@match.com", email.get());

        // Should match `alt2.eu`
        email = GitLabOAuthService.getEmail(preferredEmailMatcher, mainEmail, new String[]{
                "test1@alt.com",
                "test2@alt2.eu",
        });
        assertEquals("test2@alt2.eu", email.get());

        // Should match `alt.com`
        email = GitLabOAuthService.getEmail(preferredEmailMatcher, mainEmail, new String[]{
                "test1@alt.com",
        });
        assertEquals("test1@alt.com", email.get());

        // If nothing matches, just accept "mainEmail"
        email = GitLabOAuthService.getEmail(preferredEmailMatcher, mainEmail, new String[]{});
        assertEquals(mainEmail, email.get());

        // If multiple emails match the same pattern, return Empty
        email = GitLabOAuthService.getEmail(preferredEmailMatcher, mainEmail, new String[]{
                "test1@alt.com",
                "test2@alt.com",
        });
        assertEquals(Optional.empty(), email);
    }
}