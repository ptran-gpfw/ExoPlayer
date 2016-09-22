/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.*;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Decodes and renders video using {@link MediaCodec}.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RsVideoTrackRenderer extends MediaCodecVideoTrackRenderer {

    private static final String TAG = RsVideoTrackRenderer.class.getSimpleName();
    private ByteBuffer[] mOutputBuffers;
    private RenderScript mRs;


    public RsVideoTrackRenderer(Context context, SampleSource source, MediaCodecSelector mediaCodecSelector, int videoScalingMode, long allowedJoiningTimeMs, Handler eventHandler, EventListener eventListener, int maxDroppedFrameCountToNotify) {
        super(context, source, mediaCodecSelector, videoScalingMode, allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFrameCountToNotify);
        mRs = RenderScript.create(context);
    }

    @Override
    protected void configureCodec(MediaCodec codec, boolean codecIsAdaptive, MediaFormat format, MediaCrypto crypto) {
        maybeSetMaxInputSize(format, codecIsAdaptive);
        codec.configure(format, null, crypto, 0);

        MediaCodecInfo.CodecCapabilities caps = codec.getCodecInfo().getCapabilitiesForType("video/avc");
        Log.d(TAG, "colorFormats: " + Arrays.toString(caps.colorFormats));
    }

    @Override
    protected void releaseCodec() {
        super.releaseCodec();
        mOutputBuffers = null;
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo, int bufferIndex, boolean shouldSkip) {
        Image image = codec.getOutputImage(bufferIndex);
        if(image != null) {
            Log.d(TAG, "pob: format," + (image != null ? image.getFormat() : "null"));
            Log.d(TAG, "pob: w/h," + image.getWidth() +","+ image.getHeight());
        }

        Image.Plane[] planes = image.getPlanes();
        for(Image.Plane p : planes) {
            Log.d(TAG, "pix/row," + p.getPixelStride() +","+p.getRowStride());
            Log.d(TAG, "plan pos/rem," + p.getBuffer().position() +","+ p.getBuffer().remaining());
            //Log.d(TAG, "a0:" + p.getBuffer().get());
        }


        Type type = new Type.Builder(mRs, Element.U8_3(mRs)).create();
        Allocation allocation = Allocation.createTyped(mRs, type);

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferInfo, bufferIndex, shouldSkip);
    }
}
