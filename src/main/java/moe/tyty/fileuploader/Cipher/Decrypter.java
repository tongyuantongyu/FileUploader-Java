package moe.tyty.fileuploader.Cipher;

import moe.tyty.fileuploader.Exception.CipherInitException;
import moe.tyty.fileuploader.Exception.CipherWorkException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * class Decrypter encapsulate java's AES algorithm to provide simple call to decrypt data.
 *
 * @author TYTY
 */
public class Decrypter {
    Cipher cipher;

    public Decrypter(String password) {
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Scrypt.scrypt(password), "AES"));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new CipherInitException();
        }
    }

    /**
     * decrypt cipher
     * @param sink encrypted text
     * @return plain text
     */
    public byte[] decrypt(byte[] sink) {
        try {
            return cipher.doFinal(sink);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new CipherWorkException(e);
        }
    }
}
