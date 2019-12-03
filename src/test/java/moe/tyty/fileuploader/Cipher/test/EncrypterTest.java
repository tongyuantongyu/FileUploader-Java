package moe.tyty.fileuploader.Cipher.test;

import moe.tyty.fileuploader.Cipher.Encrypter;
import org.junit.Test;

import static org.junit.Assert.*;

public class EncrypterTest {
    @Test
    public void aes_enc_data() {
        // correct encrypted data
        byte[] correct = {
                60, 10, -1, -97, -18, -107, 60, 103,
                -123, 103, -66, -74, 54, -11, 33, -96,
                -110, 62, 86, -122, -5, -26, 115, -39,
                90, -38, 79, 37, 47, -34, -35, 91
        };

        Encrypter encrypter = new Encrypter("test");
        assertArrayEquals(correct, encrypter.encrypt("0123456789ABCDEF".getBytes()));
    }
}
