package net.typeblog.shelter.plus.vault;

import org.junit.Test;
import org.bouncycastle.util.encoders.Base64;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PinVerifierTest {
    @Test
    public void createsDistinctVersionedSaltsAndVerifiesExactPin() {
        PinVerifier verifier = new PinVerifier();
        char[] pin = "0427".toCharArray();
        String first = verifier.createRecord(pin);
        assertCleared(pin);
        char[] secondPin = "0427".toCharArray();
        String second = verifier.createRecord(secondPin);
        assertCleared(secondPin);

        assertTrue(first.startsWith("shelter-pin-v1:$argon2id$v=19$m=65536,t=3,p=1$"));
        assertNotEquals(field(first, 4), field(second, 4));
        assertEquals(16, decodeNoPadding(field(first, 4)).length);
        assertEquals(32, decodeNoPadding(field(first, 5)).length);
        char[] correct = "0427".toCharArray();
        assertTrue(verifier.verify(correct, first));
        assertCleared(correct);
        char[] shortPin = "427".toCharArray();
        assertFalse(verifier.verify(shortPin, first));
        assertCleared(shortPin);
        assertFalse(verifier.verify("0428".toCharArray(), first));
    }

    @Test
    public void rejectsNullAndUnsupportedOrMalformedRecords() {
        PinVerifier verifier = new PinVerifier();
        String record = verifier.createRecord("1234".toCharArray());

        assertFalse(verifier.verify(null, record));
        char[] nullRecordPin = "1234".toCharArray();
        assertFalse(verifier.verify(nullRecordPin, null));
        assertCleared(nullRecordPin);
        char[] malformedPin = "1234".toCharArray();
        assertFalse(verifier.verify(malformedPin, "bad"));
        assertCleared(malformedPin);
        assertFalse(verifier.verify("1234".toCharArray(), record.replace("v=19", "v=16")));
        assertFalse(verifier.verify("1234".toCharArray(), record + "extra"));
    }

    private static String field(String record, int index) {
        return record.substring("shelter-pin-v1:".length()).split("\\$", -1)[index];
    }

    private static byte[] decodeNoPadding(String value) {
        int padding = (4 - (value.length() & 3)) & 3;
        StringBuilder padded = new StringBuilder(value);
        for (int i = 0; i < padding; i++) padded.append('=');
        return Base64.decode(padded.toString());
    }

    private static void assertCleared(char[] value) {
        for (char c : value) assertEquals('\0', c);
    }
}
