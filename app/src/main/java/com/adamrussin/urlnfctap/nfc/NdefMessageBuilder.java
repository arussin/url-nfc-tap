package com.adamrussin.urlnfctap.nfc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class NdefMessageBuilder {
    private static final int MAX_NDEF_MESSAGE_BYTES = 1022;
    private static final byte URI_RECORD_TYPE = 0x55;
    private static final byte NO_URI_PREFIX_COMPRESSION = 0x00;

    private NdefMessageBuilder() {
    }

    public static byte[] buildNdefFile(String url) {
        byte[] message = buildUriMessage(url);
        ByteBuffer file = ByteBuffer.allocate(message.length + 2);
        file.putShort((short) message.length);
        file.put(message);
        return file.array();
    }

    public static byte[] buildUriMessage(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be blank");
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new IllegalArgumentException("URL must use http:// or https://");
        }

        byte[] urlBytes = url.getBytes(StandardCharsets.UTF_8);
        int payloadLength = urlBytes.length + 1;
        boolean shortRecord = payloadLength <= 255;
        int messageLength = (shortRecord ? 4 : 7) + payloadLength;
        if (messageLength > MAX_NDEF_MESSAGE_BYTES) {
            throw new IllegalArgumentException("URL is too long for the emulated NDEF file");
        }

        ByteBuffer message = ByteBuffer.allocate(messageLength);
        message.put(shortRecord ? (byte) 0xD1 : (byte) 0xC1); // MB, ME, optional SR, TNF well-known
        message.put((byte) 0x01); // Type length
        if (shortRecord) {
            message.put((byte) payloadLength);
        } else {
            message.putInt(payloadLength);
        }
        message.put(URI_RECORD_TYPE);
        message.put(NO_URI_PREFIX_COMPRESSION);
        message.put(urlBytes);
        return message.array();
    }
}
