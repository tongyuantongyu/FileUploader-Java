package moe.tyty.fileuploader.Cipher;

import com.lambdaworks.crypto.SCrypt;

import java.security.GeneralSecurityException;

/**
 * class Scrypt wraps scrypt algorithm to provide a fire and forget call to get a safe key from password.
 *
 * @author TYTY
 */
public class Scrypt {

    static final byte[] salt = "qwertyuiopasdfghjklzxcvbnm123456".getBytes();

    /**
     * generate key using password by Scrypt algorithm.
     * @param password password to be used
     * @return key generated
     * @throws GeneralSecurityException exceptions related to encryption
     */
    static public byte[] scrypt(String password) throws GeneralSecurityException {
        return SCrypt.scrypt(password.getBytes(), salt,  2, 8, 1, 32);
    }
}
