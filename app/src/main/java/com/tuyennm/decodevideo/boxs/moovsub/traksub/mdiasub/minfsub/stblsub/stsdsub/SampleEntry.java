package com.tuyennm.decodevideo.boxs.moovsub.traksub.mdiasub.minfsub.stblsub.stsdsub;

import com.tuyennm.decodevideo.boxs.Mp4Box;

/**
 * Created by yanshun.li on 10/31/16.
 */

public class SampleEntry extends Mp4Box {
    public int dataReferenceIndex;

    public SampleEntry(byte[] byteBuffer, int start) {
        super(byteBuffer, start);
        index += 6;
        dataReferenceIndex = getIntFromBuffer(byteBuffer, 2);
    }
}
