package moe.tyty.fileuploader.Protocol;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/** PACKET STRUCT OF THE PROTOCOL
 * Data packet struct
 * Client: Server Hello
 * | MAGIC_HEADER 2 | LENGTH 8 | [ Encrypted [ MAGIC_HEADER 2 | VERSION 4] ]
 *
 * Server: Client Hello
 * - Check is our header -> Close Connection
 * - Try decrypt with our key ->
 * | MAGIC_HEADER 2 | LENGTH 8 | 1 1
 * - Check version compatibility ->
 * | MAGIC_HEADER 2 | LENGTH 8 | 2 1 | [ Encrypted VERSION 4 ]
 * ( If all passed )
 * | MAGIC_HEADER 2 | LENGTH 8 | 0 1 | [ Encrypted SESSION 32 ]
 * Where SESSION is a random 32 bytes data for further file transfer
 *
 * Client: File Negotiation
 * | MAGIC_HEADER 2 | LENGTH 8 |
 * [ Encrypted [ SESSION 32 | PIECE_SIZE 8 | FILE_LENGTH 16 | FILE_PATH 256 ] ]
 * File with too long path can not be upload.
 *
 * Server: File Negotiation
 * Can't open file for write
 * | MAGIC_HEADER 2 | LENGTH 8 | [ Encrypted [ SESSION 32 | 1 1 ] ]
 * OK. Wait for data
 * | MAGIC_HEADER 2 | LENGTH 8 | [ Encrypted [ SESSION 32 | 0 1 ] ]
 *
 * Client: Start Transferring file
 *      Init data: Used to determine session.
 *      | MAGIC_HEADER_TRANSFER 2 | LENGTH 8 | [ Encrypted [ SESSION 32 ] ]
 *      Start transfer.
 *      | MAGIC_HEADER_TRANSFER 2 | LENGTH 8 |
 *      [ Encrypted [ SESSION 32 | FILE_PIECE_ORDER 8 | PIECE_SIZE 8 | FILE_PIECE PIECE_SIZE ] ]
 *      Finish transfer.
 *      | MAGIC_HEADER_TRANSFER 2 | LENGTH 8 | [ Encrypted [ SESSION 32 | 0 8 | 0 8 ] ]
 *
 */

public class Session {
    public enum MessageType {SESSION, THREAD, UNKNOWN}
    public enum HelloStatus {OK, BAD_PASSWORD, BAD_VERSION}
    public enum NegotiationStatus {OK, OPEN_FAIL}
    static Random random_source = new Random();

    public static class MsgGuess {
        public Session.MessageType type;
        public CompletableFuture<byte[]> message;
    }

    public static class ClientHelloResult {
        public HelloStatus status;
        public byte[] session;
    }

    public static class FileNegotiationInfo {
        public int pieceSize;
        public long fileLength;
        public String filePath;
    }

    /**
     * Generate random byte array of given length
     * @param length byte array length
     * @return array generated
     */
    public static byte[] session(int length) {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        for (int i = 0; i < length; ++i) {
            buffer.put(i, (byte) random_source.nextInt(256));
        }
        return buffer.array();
    }
}
