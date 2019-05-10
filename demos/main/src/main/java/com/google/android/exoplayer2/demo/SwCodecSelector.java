package com.google.android.exoplayer2.demo;

import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of [MediaCodecSelector] that allows exoplayer to instantiate an alternative decoder
 * when the OEM has restrictions on default HW decoder.
 *
 * TODO: We need a more reliable discovery of SW decoder. Currently this is done via string matching the name, see [AVC_SW_DECODER_SUFFIX].
 */
class SwCodecSelector implements MediaCodecSelector {

    /**
     * This string happens to be in the name of [android.media.MediaCodecInfo] returned on Samsung (US/Snapdragon) devices.
     */
    private static final String AVC_SW_DECODER_SUFFIX = "avc.sw.dec";

    private static final List<MediaCodecInfo> decoderInfos;

    static {
        List<MediaCodecInfo> decoderInfosTmp = null;
        try {
            decoderInfosTmp = MediaCodecUtil.getDecoderInfos(MimeTypes.VIDEO_H264, false, false);
        }
        catch (MediaCodecUtil.DecoderQueryException e) {
            e.printStackTrace();
        }
        decoderInfos = decoderInfosTmp;
    }

    @Override
    public List<MediaCodecInfo> getDecoderInfos(String mimeType, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {
        List<MediaCodecInfo> infos = new ArrayList<>();

        //If AVC decoder requested then first prefer to give the a "sw" decoder
        if (mimeType == MimeTypes.VIDEO_H264) {
            for (MediaCodecInfo decoderInfo : decoderInfos) {
                if (decoderInfo.name.endsWith(AVC_SW_DECODER_SUFFIX)) {
                    infos.add(decoderInfo);
                }
            }
        }

        //append the default decoder infos
        infos.addAll(MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder));

        return infos;
    }

    @Override
    public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
        //just use default implementation
        return MediaCodecSelector.DEFAULT.getPassthroughDecoderInfo();
    }
}

