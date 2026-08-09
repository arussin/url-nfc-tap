package com.adamrussin.urlnfctap;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import com.adamrussin.urlnfctap.nfc.NdefMessageBuilder;
import com.adamrussin.urlnfctap.nfc.NdefType4Tag;

public final class UrlHostApduService extends HostApduService {
    private final NdefType4Tag tag = new NdefType4Tag();

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (!ShareState.isActive()) {
            return new byte[]{(byte) 0x6A, (byte) 0x82};
        }
        byte[] ndefFile = NdefMessageBuilder.buildNdefFile(ShareState.selectedUrl(this));
        return tag.process(commandApdu, ndefFile);
    }

    @Override
    public void onDeactivated(int reason) {
        tag.reset();
    }
}
