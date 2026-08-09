package com.adamrussin.urlnfctap.nfc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public final class NdefMessageBuilderTest {
    @Test
    public void createsShortWellKnownUriRecord() {
        String url = "https://example.com/profile";
        byte[] message = NdefMessageBuilder.buildUriMessage(url);

        assertEquals(0xD1, message[0] & 0xFF);
        assertEquals(1, message[1] & 0xFF);
        assertEquals(url.getBytes(StandardCharsets.UTF_8).length + 1, message[2] & 0xFF);
        assertEquals(0x55, message[3] & 0xFF);
        assertEquals(0, message[4] & 0xFF);
        assertEquals(url, new String(message, 5, message.length - 5, StandardCharsets.UTF_8));
    }

    @Test
    public void ndefFileStartsWithTwoByteMessageLength() {
        byte[] file = NdefMessageBuilder.buildNdefFile("https://example.com");
        int declaredLength = ByteBuffer.wrap(file, 0, 2).getShort() & 0xFFFF;

        assertEquals(file.length - 2, declaredLength);
    }

    @Test
    public void createsNonShortRecordForLongUrl() {
        String url = "https://example.com/" + "a".repeat(300);
        byte[] message = NdefMessageBuilder.buildUriMessage(url);

        assertEquals(0xC1, message[0] & 0xFF);
        assertEquals(url.getBytes(StandardCharsets.UTF_8).length + 1,
                ByteBuffer.wrap(message, 2, 4).getInt());
        assertEquals(0x55, message[6] & 0xFF);
    }

    @Test
    public void rejectsNonWebDestinations() {
        assertThrows(IllegalArgumentException.class,
                () -> NdefMessageBuilder.buildUriMessage("mailto:someone@example.com"));
    }
}
