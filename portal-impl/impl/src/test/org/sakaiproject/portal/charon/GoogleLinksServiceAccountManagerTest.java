package org.sakaiproject.portal.charon;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-classes.xml","classpath:/fake-services.xml"})
//@ContextConfiguration(locations = "classpath:/META-INF/spring/applicationContext.xml")
public class GoogleLinksServiceAccountManagerTest {
	private static final String USER_EMAIL_ADDRESS_IN_DOMAIN =
			"test@collab.its.umich.edu";
	private static final String USER_EMAIL_ADDRESS_IN_WRONG_DOMAIN =
			"ranaseef@its.umich.edu";

	private static GoogleLinksServiceAccountManager instance = null;

	@BeforeClass
	public static void setUp() {
		instance = new GoogleLinksServiceAccountManager(
				USER_EMAIL_ADDRESS_IN_DOMAIN);
		assertNotNull(
				"Tests cannot be run without instance of the class this tests: "
				+ GoogleLinksServiceAccountManager.class.getName(),
				instance);
	}

	private ServerConfigurationService M_scs;

	public void setServerConfigurationService(ServerConfigurationService M_scs) {
		this.M_scs = M_scs;
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAccessTokenNoUser() {
		// This should through error, but not sure how it was written...
		String expected = getManager(null).getAccessToken();
	}

	@Test
	public void testGetAccessTokenUserWrongDomain() {
		// This should through error, but not sure how it was written...
		String expected = 
				getManager(USER_EMAIL_ADDRESS_IN_WRONG_DOMAIN).getAccessToken();
		assertNull("There should be no access token, as the user email address is in the wrong domain", expected);
	}

	@Test
	public void testGetAccessToken() {
		// This should through error, but not sure how it was written...
		String expected =
				getManager(USER_EMAIL_ADDRESS_IN_DOMAIN).getAccessToken();
		// Commented out 2013-04-01: this unit test does not work correctly, as there is no mockup of Google services
		//assertNotNull("Access token must not be null, as the user email address is null", expected);
	}

	private GoogleLinksServiceAccountManager getManager(String userEmailAddress)
	{
		return new GoogleLinksServiceAccountManager(userEmailAddress);
	}
}
