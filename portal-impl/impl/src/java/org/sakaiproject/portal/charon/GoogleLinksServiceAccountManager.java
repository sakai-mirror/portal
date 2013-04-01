package org.sakaiproject.portal.charon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.google.impl.SakaiGoogleAuthServiceImpl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.plus.PlusScopes;

/**
 * This manages configuration of Google Service Account used in Sakai for Google
 * Links.
 * 
 * @author ranaseef
 *
 */
public class GoogleLinksServiceAccountManager {
	// Constants ----------------------------------------------------

	private static final Log M_log =
			LogFactory.getLog(GoogleLinksServiceAccountManager.class);
	// Only instance of this singleton factory
	private static final GoogleLinksServiceAccountManager INSTANCE =
			new GoogleLinksServiceAccountManager();

	// Static public methods ----------------------------------------

	public static GoogleLinksServiceAccountManager getInstance() {
		return INSTANCE;
	}


	// Instance variables -------------------------------------------



	// Constructors -------------------------------------------------

	private GoogleLinksServiceAccountManager() {
	}


	// Public methods -----------------------------------------------

	public String getAccessToken(String userEmailAddress) {
		String result = null;
		// CalendarScopes.CALENDAR_READONLY
		String[] scopes = new String[] {
				CalendarScopes.CALENDAR,
				DriveScopes.DRIVE,
				"https://mail.google.com/mail/feed/atom/",
				PlusScopes.PLUS_ME
			};
		// TODO: Validate serviceAccount
		GoogleServiceAccount serviceAccount =
				new GoogleServiceAccount("google.links.readWrite");
		try {
			GoogleCredential credential = SakaiGoogleAuthServiceImpl.authorize(
					userEmailAddress,
					serviceAccount.getEmailAddress(),
					serviceAccount.getPrivateKeyFilePath(),
					scopes);
			if (credential != null) {
				credential.refreshToken();
				result = credential.getAccessToken();
			} else {
				M_log.warn("Unable to get credential for " + serviceAccount);
			}
		} catch (Exception err) {
			M_log.error("Failed to get access token for \"" + userEmailAddress + "\"", err);
		}
		return result;
	}
}
