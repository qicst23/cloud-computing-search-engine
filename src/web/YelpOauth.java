/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */

package web;

import org.scribe.model.Token;
import org.scribe.builder.api.DefaultApi10a;


/**
 * Service provider for "2-legged" OAuth10a for Yelp API (version 2).
 */
public class YelpOauth extends DefaultApi10a {

  @Override
  public String getAccessTokenEndpoint() {
    return null;
  }

  @Override
  public String getAuthorizationUrl(Token arg0) {
    return null;
  }

  @Override
  public String getRequestTokenEndpoint() {
    return null;
  }

}