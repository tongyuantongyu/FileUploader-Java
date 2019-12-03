package moe.tyty.fileuploader.Cipher;

import moe.tyty.fileuploader.Exception.CipherInitException;
import moe.tyty.fileuploader.Exception.CipherWorkException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

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

    public byte[] decrypt(byte[] sink) {
        try {
            return cipher.doFinal(sink);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new CipherWorkException();
        }
    }

    public static void main(String[] args) {
        Decrypter dec = new Decrypter("test");
        byte[] sink = {
                60, 10, -1, -97, -18, -107, 60, 103,
                -123, 103, -66, -74, 54, -11, 33, -96,
                -110, 62, 86, -122, -5, -26, 115, -39,
                90, -38, 79, 37, 47, -34, -35, 91
        };
        System.out.println(new String(dec.decrypt(sink)));
    }
}
