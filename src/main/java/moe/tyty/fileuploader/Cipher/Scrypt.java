package moe.tyty.fileuploader.Cipher;

import com.lambdaworks.crypto.SCrypt;

import java.security.GeneralSecurityException;

public class Scrypt {

    static byte[] salt = "qwertyuiopasdfghjklzxcvbnm123456".getBytes();

    static public byte[] scrypt(String password) throws GeneralSecurityException {
        return SCrypt.scrypt(password.getBytes(), salt,  2, 8, 1, 32);
    }
}
