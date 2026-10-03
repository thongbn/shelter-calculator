package net.typeblog.shelter.plus.vault;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/** In-memory Argon2id PIN verifier. This class does not persist or log PIN data. */
public final class PinVerifier {
    private static final int ITERATIONS = 3;
    private static final int MEMORY_KIB = 64 * 1024;
    private static final int PARALLELISM = 1;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final String RECORD_PREFIX = "shelter-pin-v1:";
    private static final String PHC_PREFIX = "$argon2id$v=19$m=" + MEMORY_KIB
            + ",t=" + ITERATIONS + ",p=" + PARALLELISM + "$";
    private static final int MAX_RECORD_CHARS = 256;

    private final SecureRandom random;

    public PinVerifier() {
        this(new SecureRandom());
    }

    PinVerifier(SecureRandom random) {
        if (random == null) throw new IllegalArgumentException("random must not be null");
        this.random = random;
    }

    /** Creates a versioned PHC record using a fresh random salt, then clears the input array. */
    public String createRecord(char[] pin) {
        if (pin == null) throw new IllegalArgumentException("PIN must not be null");
        try {
            byte[] password = encodePin(pin);
            byte[] salt = new byte[SALT_BYTES];
            byte[] hash = new byte[HASH_BYTES];
            try {
                random.nextBytes(salt);
                derive(password, salt, hash);
                return RECORD_PREFIX + PHC_PREFIX + encodeNoPadding(salt)
                        + "$" + encodeNoPadding(hash);
            } finally {
                wipe(password);
                wipe(salt);
                wipe(hash);
            }
        } finally {
            wipe(pin);
        }
    }

    /** Verifies an exact UTF-8 PIN match and clears the caller's input array on every path. */
    public boolean verify(char[] pin, String record) {
        try {
            if (pin == null || record == null || record.length() > MAX_RECORD_CHARS
                    || !record.startsWith(RECORD_PREFIX)) return false;
            byte[][] parsed = parseRecord(record.substring(RECORD_PREFIX.length()));
            if (parsed == null) return false;

            byte[] password = null;
            byte[] actual = new byte[HASH_BYTES];
            try {
                password = encodePin(pin);
                derive(password, parsed[0], actual);
                return Arrays.constantTimeAreEqual(parsed[1], actual);
            } catch (IllegalArgumentException malformedPin) {
                return false;
            } finally {
                wipe(password);
                wipe(actual);
                wipe(parsed[0]);
                wipe(parsed[1]);
            }
        } finally {
            wipe(pin);
        }
    }

    private static void derive(byte[] password, byte[] salt, byte[] output) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ITERATIONS)
                .withMemoryAsKB(MEMORY_KIB)
                .withParallelism(PARALLELISM)
                .withSalt(salt)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        try {
            generator.init(parameters);
            generator.generateBytes(password, output);
        } finally {
            parameters.clear();
        }
    }

    private static byte[] encodePin(char[] pin) {
        char[] copy = pin.clone();
        ByteBuffer encoded = null;
        try {
            encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(copy));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("PIN contains invalid Unicode", malformed);
        } finally {
            java.util.Arrays.fill(copy, '\0');
            if (encoded != null && encoded.hasArray()) wipe(encoded.array());
        }
    }

    private static byte[][] parseRecord(String encoded) {
        String[] fields = encoded.split("\\$", -1);
        if (fields.length != 6 || !fields[0].isEmpty() || !"argon2id".equals(fields[1])
                || !"v=19".equals(fields[2]) || !("m=" + MEMORY_KIB + ",t=" + ITERATIONS
                + ",p=" + PARALLELISM).equals(fields[3])
                || fields[4].length() != 22 || fields[5].length() != 43
                || !isBase64Field(fields[4]) || !isBase64Field(fields[5])) {
            return null;
        }
        try {
            byte[] salt = decodeNoPadding(fields[4]);
            byte[] hash = decodeNoPadding(fields[5]);
            if (salt.length != SALT_BYTES || hash.length != HASH_BYTES) {
                wipe(salt);
                wipe(hash);
                return null;
            }
            return new byte[][] { salt, hash };
        } catch (IllegalArgumentException invalidBase64) {
            return null;
        }
    }

    static boolean isRecordSupported(String record) {
        if (record == null || record.length() > MAX_RECORD_CHARS
                || !record.startsWith(RECORD_PREFIX)) return false;
        byte[][] parsed = parseRecord(record.substring(RECORD_PREFIX.length()));
        if (parsed == null) return false;
        wipe(parsed[0]);
        wipe(parsed[1]);
        return true;
    }

    private static boolean isBase64Field(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z')
                    && !(c >= '0' && c <= '9') && c != '+' && c != '/') return false;
        }
        return true;
    }

    private static String encodeNoPadding(byte[] value) {
        byte[] encoded = Base64.encode(value);
        try {
            int length = encoded.length;
            while (length > 0 && encoded[length - 1] == '=') length--;
            return new String(encoded, 0, length, StandardCharsets.US_ASCII);
        } finally {
            wipe(encoded);
        }
    }

    private static byte[] decodeNoPadding(String value) {
        int padding = (4 - (value.length() & 3)) & 3;
        StringBuilder padded = new StringBuilder(value.length() + padding).append(value);
        for (int i = 0; i < padding; i++) padded.append('=');
        return Base64.decode(padded.toString());
    }

    private static void wipe(byte[] value) {
        if (value != null) java.util.Arrays.fill(value, (byte) 0);
    }

    private static void wipe(char[] value) {
        if (value != null) java.util.Arrays.fill(value, '\0');
    }
}
