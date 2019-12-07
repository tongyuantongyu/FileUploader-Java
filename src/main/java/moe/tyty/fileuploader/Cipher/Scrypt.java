package moe.tyty.fileuploader.Cipher;

import com.lambdaworks.crypto.SCrypt;

import java.security.GeneralSecurityException;

public class Scrypt {

    static byte[] salt = "qwertyuiopasdfghjklzxcvbnm123456".getBytes();

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
