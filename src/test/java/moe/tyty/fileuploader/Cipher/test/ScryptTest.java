package moe.tyty.fileuploader.Cipher.test;

import moe.tyty.fileuploader.Cipher.Scrypt;
import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.junit.Assert.*;

public class ScryptTest {

    @Test
    public void scrypt_gen_key() throws GeneralSecurityException {
        // correct key derived by scrypt using password "test"
        byte[] correct = {
                -85, 114, 19, -101, -80, 51, 66, 87,
                92, 57, 44, 33, -5, -93, 105, -4,
                -68, -116, 5, -60, -23, -64, -34, 102,
                53, -51, -30, 35, -28, -51, -116, 76
        };

        assertArrayEquals(correct, Scrypt.scrypt("test"));
    }
}
