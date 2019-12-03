package moe.tyty.fileuploader.Cipher;

import moe.tyty.fileuploader.Exception.CipherInitException;
import moe.tyty.fileuploader.Exception.CipherWorkException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class Encrypter {
    Cipher cipher;

    public Encrypter(String password) {
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Scrypt.scrypt(password), "AES"));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new CipherInitException();
        }
    }

    public byte[] encrypt(byte[] plain) {
        try {
            return cipher.doFinal(plain);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new CipherWorkException();
        }
    }
}
