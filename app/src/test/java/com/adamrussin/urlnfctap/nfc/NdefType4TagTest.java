package com.adamrussin.urlnfctap.nfc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

public final class NdefType4TagTest {
    @Test
    public void readerCanSelectAndReadNdefFile() {
        NdefType4Tag tag = new NdefType4Tag();
        byte[] ndefFile = NdefMessageBuilder.buildNdefFile("https://example.com/profile");

        assertStatusSuccess(tag.process(hex("00A4040007D276000085010100"), ndefFile));
        assertStatusSuccess(tag.process(hex("00A4000C02E104"), ndefFile));

        byte[] lengthResponse = tag.process(hex("00B0000002"), ndefFile);
        int messageLength = ByteBuffer.wrap(lengthResponse, 0, 2).getShort() & 0xFFFF;
        assertEquals(ndefFile.length - 2, messageLength);
        assertStatusSuccess(lengthResponse);

        byte[] messageResponse = tag.process(
                hex(String.format("00B00002%02X", messageLength)), ndefFile);
        assertArrayEquals(Arrays.copyOfRange(ndefFile, 2, ndefFile.length),
                Arrays.copyOf(messageResponse, messageResponse.length - 2));
        assertStatusSuccess(messageResponse);
    }

    @Test
    public void readerCanReadCapabilityContainerInChunks() {
        NdefType4Tag tag = new NdefType4Tag();
        byte[] ndefFile = NdefMessageBuilder.buildNdefFile("https://example.com");

        assertStatusSuccess(tag.process(hex("00A4040007D276000085010100"), ndefFile));
        assertStatusSuccess(tag.process(hex("00A4000C02E103"), ndefFile));

        byte[] firstChunk = tag.process(hex("00B0000007"), ndefFile);
        assertArrayEquals(hex("000F20003B0034"), Arrays.copyOf(firstChunk, 7));
        assertStatusSuccess(firstChunk);

        byte[] secondChunk = tag.process(hex("00B0000708"), ndefFile);
        assertArrayEquals(hex("0406E104040000FF"), Arrays.copyOf(secondChunk, 8));
        assertStatusSuccess(secondChunk);
    }

    @Test
    public void readBeforeFileSelectionIsRejected() {
        NdefType4Tag tag = new NdefType4Tag();
        byte[] response = tag.process(
                hex("00B0000002"), NdefMessageBuilder.buildNdefFile("https://example.com"));

        assertArrayEquals(hex("6986"), response);
    }

    private static void assertStatusSuccess(byte[] response) {
        assertEquals(0x90, response[response.length - 2] & 0xFF);
        assertEquals(0x00, response[response.length - 1] & 0xFF);
    }

    private static byte[] hex(String value) {
        byte[] output = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            output[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return output;
    }
}
