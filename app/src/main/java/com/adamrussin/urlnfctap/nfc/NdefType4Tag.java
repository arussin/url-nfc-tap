package com.adamrussin.urlnfctap.nfc;

import java.util.Arrays;

public final class NdefType4Tag {
    private static final byte[] NDEF_APPLICATION_AID = hex("D2760000850101");
    private static final byte[] CAPABILITY_CONTAINER_FILE_ID = hex("E103");
    private static final byte[] NDEF_FILE_ID = hex("E104");

    private static final byte[] CAPABILITY_CONTAINER = hex(
            "000F" + // Capability Container length
            "20" +   // Mapping version 2.0
            "003B" + // Maximum response data size
            "0034" + // Maximum command data size
            "04" +   // NDEF File Control TLV
            "06" +   // TLV length
            "E104" + // NDEF file ID
            "0400" + // Maximum NDEF file size: 1024 bytes
            "00" +   // Read access: free
            "FF"     // Write access: never
    );

    private static final byte[] SUCCESS = hex("9000");
    private static final byte[] FILE_NOT_FOUND = hex("6A82");
    private static final byte[] WRONG_LENGTH = hex("6700");
    private static final byte[] WRONG_PARAMETERS = hex("6B00");
    private static final byte[] COMMAND_NOT_ALLOWED = hex("6986");
    private static final byte[] INSTRUCTION_NOT_SUPPORTED = hex("6D00");

    private SelectedFile selectedFile = SelectedFile.NONE;

    public byte[] process(byte[] command, byte[] ndefFile) {
        if (command == null || command.length < 4) return WRONG_LENGTH.clone();

        int instruction = command[1] & 0xFF;
        if (instruction == 0xA4) return processSelect(command);
        if (instruction == 0xB0) return processReadBinary(command, ndefFile);
        if (instruction == 0xD6) return COMMAND_NOT_ALLOWED.clone();
        return INSTRUCTION_NOT_SUPPORTED.clone();
    }

    public void reset() {
        selectedFile = SelectedFile.NONE;
    }

    private byte[] processSelect(byte[] command) {
        if (command.length < 5) return WRONG_LENGTH.clone();

        int p1 = command[2] & 0xFF;
        int dataLength = command[4] & 0xFF;
        if (command.length < 5 + dataLength) return WRONG_LENGTH.clone();
        byte[] data = Arrays.copyOfRange(command, 5, 5 + dataLength);

        if (p1 == 0x04 && Arrays.equals(data, NDEF_APPLICATION_AID)) {
            selectedFile = SelectedFile.NONE;
            return SUCCESS.clone();
        }

        if (p1 == 0x00 && Arrays.equals(data, CAPABILITY_CONTAINER_FILE_ID)) {
            selectedFile = SelectedFile.CAPABILITY_CONTAINER;
            return SUCCESS.clone();
        }

        if (p1 == 0x00 && Arrays.equals(data, NDEF_FILE_ID)) {
            selectedFile = SelectedFile.NDEF;
            return SUCCESS.clone();
        }

        return FILE_NOT_FOUND.clone();
    }

    private byte[] processReadBinary(byte[] command, byte[] ndefFile) {
        if (command.length < 5) return WRONG_LENGTH.clone();
        if (selectedFile == SelectedFile.NONE) return COMMAND_NOT_ALLOWED.clone();

        byte[] file = selectedFile == SelectedFile.CAPABILITY_CONTAINER
                ? CAPABILITY_CONTAINER
                : ndefFile;
        int offset = ((command[2] & 0xFF) << 8) | (command[3] & 0xFF);
        int requestedLength = command[4] & 0xFF;
        if (requestedLength == 0) requestedLength = 256;
        if (offset > file.length) return WRONG_PARAMETERS.clone();

        int responseLength = Math.min(requestedLength, file.length - offset);
        byte[] response = new byte[responseLength + SUCCESS.length];
        System.arraycopy(file, offset, response, 0, responseLength);
        System.arraycopy(SUCCESS, 0, response, responseLength, SUCCESS.length);
        return response;
    }

    private static byte[] hex(String value) {
        int length = value.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return result;
    }

    private enum SelectedFile {
        NONE,
        CAPABILITY_CONTAINER,
        NDEF
    }
}
