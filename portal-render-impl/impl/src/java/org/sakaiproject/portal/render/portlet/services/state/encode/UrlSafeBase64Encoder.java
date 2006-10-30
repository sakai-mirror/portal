package org.sakaiproject.portal.render.portlet.services.state.encode;

import org.apache.commons.codec.binary.Base64;

/**
 * Encoder 
 */
public class UrlSafeBase64Encoder implements UrlSafeEncoder {

    static final char[][] REPLACEMENTS = new char[][]{
            {'=', '_'},
            {'+', '-'},
            {'/', '!'}
    };

    public String encode(byte[] bits) {
        byte[] encoded = Base64.encodeBase64(bits);
        String unsafe = new String(encoded);
        for (int i = 0; i < REPLACEMENTS.length; i++) {
            unsafe = unsafe.replace(REPLACEMENTS[i][0], REPLACEMENTS[i][1]);
        }
        return unsafe;
    }


    public byte[] decode(String safe) {
        String unsafe = safe;
        for (int i = 0; i < REPLACEMENTS.length; i++) {
            unsafe = unsafe.replace(REPLACEMENTS[i][1], REPLACEMENTS[i][0]);
        }
        return Base64.decodeBase64(unsafe.getBytes());
    }
}
