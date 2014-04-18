package edu.osu.cse.ai.photoclassifier;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.groups.Group;
import com.flickr4java.flickr.groups.GroupsInterface;
import com.flickr4java.flickr.util.FileAuthStore;
import org.junit.Assert;
import org.junit.Ignore;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import sun.management.FileSystem;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.Scanner;

/**
 * Created by fathi on 4/11/14.
 */
public class TestGroupImageFetcher {

    private final String API_KEY;
    private final String SHARED_SECRET;
    private final static String BASE_DIR = "scratch";


    static {
        Path path = FileSystems.getDefault().getPath("api-key");
        String[] lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        API_KEY = lines[0].substring("API key=".length())
        SHARED_SECRET = lines[1].substring("Secret=".length())
    }
    public TestGroupImageFetcher(){

    }


    @Ignore("This is not needed now")
    public void testFlickrObject() {
        try {
            Flickr flickr = new Flickr(API_KEY, SHARED_SECRET, new REST());

            File authenticationDir = new File(System.getProperty("user.home"), ".flickerAppAuthentication");
            Auth auth;
            if (authenticationDir.exists()) {
                FileAuthStore authStore = new FileAuthStore(authenticationDir);
                auth = authStore.retrieveAll()[0];
            } else {
                AuthInterface authInterface = flickr.getAuthInterface();
                Token requestToken = authInterface.getRequestToken();
                String authorizationUrl = authInterface.getAuthorizationUrl(requestToken, Permission.READ);
                System.out.println("Follow this URL to authorise yourself on Flickr");
                System.out.println(authorizationUrl);
                System.out.println("Paste in the token it gives you:");
                System.out.print(">>");

                String tokenKey = new Scanner(System.in).nextLine();

                Token accessToken = authInterface.getAccessToken(requestToken, new Verifier(tokenKey));
                auth = authInterface.checkToken(accessToken);
                RequestContext.getRequestContext().setAuth(auth);
                FileAuthStore authStore = new FileAuthStore(authenticationDir);
                authStore.store(auth);
                System.out.println("Thanks.  You probably will not have to do this every time.  Now starting backup.");
            }
            GroupsInterface groupsInterface = flickr.getGroupsInterface();
            String[] groupKeywords = {"landscape", "flowers", "portraits"};
            for (String keyword : groupKeywords) {
                Collection<Group> groups = groupsInterface.search(keyword, 10, 1);
                for (Group group : groups) {
                    System.out.printf("new GroupInfo(\"%s\", \"%s\", \"%s\"), ", group.getId(), keyword, group.getName());
                    System.out.println();
                }
            }

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
