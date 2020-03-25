package com.tuyennm.decodevideo.boxs.moovsub.traksub.mdiasub.minfsub.stblsub;

import com.tuyennm.decodevideo.boxs.FullBox;

/**
 * Created by yanshun.li on 10/26/16.
 */

public class StszBox extends FullBox {
    private final int sampleSize;
    public int[] sampleCount;

    public StszBox(byte[] byteBuffer, int start) {
        super(byteBuffer, start);
        sampleSize = getIntFromBuffer(byteBuffer, 4);
        sampleCount = new int[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            sampleCount[i] = getIntFromBuffer(byteBuffer, 4);
        }
    }
}
