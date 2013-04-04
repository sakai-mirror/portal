package org.sakaiproject.portal.charon;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.google.impl.SakaiGoogleAuthServiceImpl;
import org.sakaiproject.portal.api.PortalRenderContext;

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


	// Instance variables -------------------------------------------

	private String userEmailAddress;
	private GoogleCredential googleCredential;
	private GoogleServiceAccount googleServiceAccount;
	// CalendarScopes.CALENDAR_READONLY
	private String[] scopes = new String[] {
			CalendarScopes.CALENDAR,
			DriveScopes.DRIVE,
			"https://mail.google.com/mail/feed/atom/",
			PlusScopes.PLUS_ME
		};


	// Constructors -------------------------------------------------

	/**
	 * TODO: decide what to do about filtering -- should this throw error if the
	 * email address is not valid (i.e. must be "@umich.edu")?
	 */
	public GoogleLinksServiceAccountManager(String userEmailAddress) {
		// TODO: find "ObjectUtility" method to perform this test...
		if ((userEmailAddress == null) || "".equals(userEmailAddress.trim())) {
			throw new IllegalArgumentException(
					"Constructor requires userEmailAddress not blank.");
		}
		setUserEmailAddress(userEmailAddress);
		// TODO: manage service account somewhere else; perhaps inject it
		setGoogleServiceAccount(
				new GoogleServiceAccount("google.links.readWrite"));
	}


	// Public methods -----------------------------------------------

	public String getAccessToken() {
		String result = null;
		try {
			GoogleCredential credential = findGoogleCredential();
			if (credential != null) {
				credential.refreshToken();
				result = credential.getAccessToken();
			} else {
				M_log.warn(
						"Unable to get credential for "
						+ getGoogleServiceAccount());
			}
		} catch (Exception err) {
			M_log.error(
					"Failed to get access token for \""
					+ getUserEmailAddress()
					+ "\"",
					err);
		}
		return result;
	}

	/**
	 * Returns the ID of the user's personal calendar.  Testing shows this is
	 * the user's email address.
	 */
	public String getUserCalendarId() {
		return getUserEmailAddress();
	}

	public String getUserEmailAddress() {
		return userEmailAddress;
	}

	public void setGoogleContextVariables(
			PortalRenderContext rcontext,
			HttpSession httpSession)
	{
		String googleLinksAccessToken = getAccessToken();
		rcontext.put("googleLinksAccessToken", googleLinksAccessToken);
		String googleCalendarId = getUserCalendarId();
		rcontext.put("userGoogleCalendarId", googleCalendarId);
		rcontext.put(
				 "googleLinksDriveDocsMaximumAgeDays",
				 getDriveDocsMaximumAgeDays());
	}


	// Private methods ----------------------------------------------

	private void authorize() {
		if (getGoogleCredential() != null) {
			return;	// Quick return: already authorized - there is nothing to do
		}
		try {
			setGoogleCredential(SakaiGoogleAuthServiceImpl.authorize(
					getUserEmailAddress(),
					getGoogleServiceAccount().getEmailAddress(),
					getGoogleServiceAccount().getPrivateKeyFilePath(),
					getScopes()));
		} catch (Exception err) {
			M_log.error(
					"Unable to authorize "
					+ getGoogleServiceAccount()
					+ " with \""
					+ getUserEmailAddress()
					+ "\"",
					err);
		}
	}

	// TODO: store this in field instead of repeating lookup?
	private int getDriveDocsMaximumAgeDays() {
		return ServerConfigurationService
				.getInt("google.links.drive.docs.maximum.age.days",
				14);
	}

	private GoogleCredential findGoogleCredential() {
		authorize();
		return getGoogleCredential();
	}

	private GoogleCredential getGoogleCredential() {
		return googleCredential;
	}

	private void setGoogleCredential(GoogleCredential value) {
		googleCredential = value;
	}

	private GoogleServiceAccount getGoogleServiceAccount() {
		return googleServiceAccount;
	}

	private void setGoogleServiceAccount(GoogleServiceAccount value) {
		googleServiceAccount = value;
	}

	private void setUserEmailAddress(String value) {
		userEmailAddress = value;
	}

	private String[] getScopes() {
		return scopes;
	}
}
