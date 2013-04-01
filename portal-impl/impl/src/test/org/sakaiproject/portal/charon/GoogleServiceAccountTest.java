package org.sakaiproject.portal.charon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

public class GoogleServiceAccountTest {
/*	@Test
	public void testGoogleServiceAccountString() {
		String serviceAccountPropertiesPrefix = "GoogleServiceUnitTestFake";
		String expectedEmailAddress = "fakeEmailAddress@nowhere.com";
		String expectedFilePath = "/fake/path/in/properties";
		Map<String, String> setProps = new HashMap<String, String>();
		setProps.put(
				serviceAccountPropertiesPrefix
				+ GoogleServiceAccount.PROPERTY_SUFFIX_EMAIL_ADDRESS,
				expectedEmailAddress);
		setProps.put(
				serviceAccountPropertiesPrefix
				+ GoogleServiceAccount.PROPERTY_SUFFIX_PRIVATE_KEY_FILE_PATH,
				expectedFilePath);
		List<String> addedPropertyKeys = null;
		try {
			GoogleServiceAccount serviceAccount =
					new GoogleServiceAccount(serviceAccountPropertiesPrefix);
			assertEquals(
					expectedEmailAddress,
					serviceAccount.getEmailAddress());
			assertEquals(
					expectedFilePath,
					serviceAccount.getPrivateKeyFilePath());
		} finally {
		}
	}
*/
	@Test
	public void testGoogleServiceAccountStringString() {
		String expectedEmailAddress = "test@GoogleServiceAccount";
		String expectedPrivateKeyFilePath = "/not/real/path.p12";
		GoogleServiceAccount serviceAccount = new GoogleServiceAccount(
				expectedEmailAddress,
				expectedPrivateKeyFilePath);
		assertEquals(expectedEmailAddress, serviceAccount.getEmailAddress());
		assertEquals(
				expectedPrivateKeyFilePath,
				serviceAccount.getPrivateKeyFilePath());
	}
}
