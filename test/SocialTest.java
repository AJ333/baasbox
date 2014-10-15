import static play.test.Helpers.PUT;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.UUID;

import org.junit.Test;

import play.Logger;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;

import com.baasbox.security.SessionKeys;
import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractTest;
import core.TestConfig;


public class SocialTest extends AbstractTest {

	
		@Test
		public void testSocial(){
			running	(
				getFakeApplication(), 
				new Runnable() {
					public void run() 	{
						//after a Signup using a social network, the user data are returned, issue #504
							// Prepare test user
							String o_token=UUID.randomUUID().toString();
							JsonNode node = updatePayloadFieldValue("/socialSignup.json", "oauth_token", o_token);
	
							// Create user
							FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withJsonBody(node, getMethod());
							Result result = routeAndCall(request);						
							assertRoute(result, "testSocial - check user no rid", 200, "\"user\":{\"name\":", true);
							
							String body = play.test.Helpers.contentAsString(result);
							JsonNode jsonRes = Json.parse(body);
							String token = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
							String username = jsonRes.get("data").get("user").get("name").textValue();
						
						//there is the signupdate in system object?
							String url="/users?fields=system%20as%20s&where=user.name%3D%22"+username+"%22";
							Logger.debug("URL to check signupdate in system: " + url);
							request = new FakeRequest("GET",url);
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_TOKEN, token);
							result = routeAndCall(request);						
							assertRoute(result, "testSocial - signupdate in system", 200, "\"signUpDate\":\"", true);
						
						//users cannot access to system properties belonging to other users
							String sFakeUser = "testsocialnormaluser_" + UUID.randomUUID();
							// Prepare test user
							node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
	
							// Create user
							request = new FakeRequest("POST", "/user");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withJsonBody(node, "POST");
							result = routeAndCall(request);
							assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUser+"\"", true);

							url="/users?fields=system%20as%20s&where=user.name%3D%22"+username+"%22";
							Logger.debug("URL to check signupdate in system: " + url);
							request = new FakeRequest("GET",url);
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUser, "passw1"));
							result = routeAndCall(request);						
							assertRoute(result, "testSocial - no access to system attribute by other users", 200, "s\":null", true);
						
						//users have social data property	
							url="/users?&where=user.name%3D%22"+username+"%22";
							request = new FakeRequest("GET",url);
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUser, "passw1"));
							result = routeAndCall(request);						
							assertRoute(result, "testSocial - visibleByRegisteredUsers._social must be present", 200, "\"visibleByRegisteredUsers\":{\"_social\":{\"facebook\":{\"id\"", true);
							
							
						//users cannot update their _social fields
							request = new FakeRequest("PUT", "/me");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_TOKEN, token);
							request = request.withJsonBody(getPayload("/adminUserUpdateNoRolePayload.json"), PUT);
							result = routeAndCall(request);
							assertRoute(result, "testSocial - cannot update their _social fields", 200, "\"_social\":{\"facebook\":{\"id\":\"mockid", true);
							
						
					}
				}
			);		
		}

	

		@Override
		public String getRouteAddress() {
			return "/social/facebook";
		}

		@Override
		public String getMethod() {
			return "POST";
		}

		@Override
		protected void assertContent(String s) {
			// TODO Auto-generated method stub
			
		}
		
		
		
		
		
	


}
