package org.sakaiproject.portal.charon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ServerConfigurationService;

/**
 * Google Service Account information for authorizing with Google.
 * 
 * The properties for authorizing include:
 * <ul>
 * 	<li>Service Account's email address</li>
 * 	<li>Service Account's private key file path (.p12)</li>
 * 	<li>
 * 		Scopes the service account will need; this may be removed, as scopes may
 * 		be specific to the request in hand (e.g., one person may need read-write
 * 		access, while another may only request read access)
 * 	</li>
 * </ul>
 * 
 * @author ranaseef
 *
 */
public class GoogleServiceAccount {
	// Constants ----------------------------------------------------

	private static final Log M_log =
			LogFactory.getLog(GoogleServiceAccount.class);

	static final String PROPERTY_SUFFIX_EMAIL_ADDRESS =
			".service.account.email.address";
	static final String PROPERTY_SUFFIX_PRIVATE_KEY_FILE_PATH =
			".service.account.private.key.file";


	// Instance variables -------------------------------------------

	private String emailAddress;
	private String privateKeyFilePath;
	// Called SCOPES as this will be changed into String[] listing all the
	// scopes for the service account
	private String scopes =
			"https://mail.google.com/mail/feed/atom/";
	private String propertiesPrefix;


	// Constructors -------------------------------------------------

	/**
	 * Use this in production, getting configuration for this service account
	 * from system properties.
	 * 
	 * @param propertiesPrefix Prefix for properties, critical to keep
	 * properties separate for each service account.
	 */
	public GoogleServiceAccount(String propertiesPrefix) {
		setPropertiesPrefix(propertiesPrefix);
	}

	/**
	 * Constructor setting properties directly; this is for unit testing only.
	 * If this method is called from anywhere else, that is an error.
	 * 
	 * @param emailAddress	Service Account's email address.
	 * @param privateKeyFilePath Pathname for account's .p12 file.
	 */
	public GoogleServiceAccount(String emailAddress, String privateKeyFilePath)
	{
		M_log.error(
				"This GoogleServiceAccount constructor is for unit testing and "
				+ "not proper in production.");
		setEmailAddress(emailAddress);
		setPrivateKeyFilePath(privateKeyFilePath);
	}


	// Public methods -----------------------------------------------

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getPrivateKeyFilePath() {
		return privateKeyFilePath;
	}

	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}

	public String getScopes() {
		return scopes;
	}

	public String toString() {
		return
				"GoogleServiceAccount [propPrefix=\""
				+ getPropertiesPrefix()
				+ "\", emailAddress=\""
				+ getEmailAddress()
				+ "\", p12FilePath=\""
				+ getPrivateKeyFilePath()
				+ "\"]";
	}


	// Protected methods --------------------------------------------

	protected void setEmailAddress(String value) {
		emailAddress = value;
	}

	protected void setPrivateKeyFilePath(String value) {
		privateKeyFilePath = value;
	}


	// Private methods ----------------------------------------------

	/**
	 * Sets prefix used to get values from properties.  This automatically gets
	 * those values immediately
	 */
	private void setPropertiesPrefix(String value) {
		// TODO: Use common class method to do this logic check: isEmpty(value)
		if ((value == null) || (value.trim() == "")) {
			throw new IllegalArgumentException(
					"Property prefix for GoogleServiceAccount must not be "
					+ "empty.");
		}
		propertiesPrefix = value;
		loadProperties();
	}

	/**
	 * Get account service's properties and store them internally.
	 */
	private void loadProperties() {
		setEmailAddress(getStringProperty(PROPERTY_SUFFIX_EMAIL_ADDRESS));
		setPrivateKeyFilePath(
				getStringProperty(PROPERTY_SUFFIX_PRIVATE_KEY_FILE_PATH));
	}

	/**
	 * This method is responsible for getting properties from system for this
	 * service account.
	 */
	private String getStringProperty(String suffix) {
		return ServerConfigurationService
				.getString(getPropertiesPrefix() + suffix);
	}
}
