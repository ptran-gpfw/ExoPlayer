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
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Decodes and renders video using {@link MediaCodec}.
 */
@TargetApi(Build.VERSION_CODES.N)
public class RsVideoTrackRenderer extends MediaCodecVideoTrackRenderer {

    private static final String TAG = RsVideoTrackRenderer.class.getSimpleName();

    // just hardcoding these for now, just trying to work with Big Buck Bunny video, assuming YUV420 and 1280x720
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private ByteBuffer[] mOutputBuffers;
    private RenderScript mRs;
    private Allocation mRenderAllocation;

    public RsVideoTrackRenderer(Context context, SampleSource source, MediaCodecSelector mediaCodecSelector, int videoScalingMode, long allowedJoiningTimeMs, Handler eventHandler, EventListener eventListener, int maxDroppedFrameCountToNotify) {
        super(context, source, mediaCodecSelector, videoScalingMode, allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFrameCountToNotify);
        mRs = RenderScript.create(context);
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        if (messageType == MSG_SET_SURFACE) {
            Log.d(TAG, "SET SURFACE!!");
            Surface surface = (Surface)message;
            mRenderAllocation = createRenderAllocation(surface,
                                                       WIDTH,
                                                       HEIGHT);
        }
        super.handleMessage(messageType, message);
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
        int totalFromPlanes = 0;
        for(Image.Plane p : planes) {
            Log.d(TAG, "pix/row," + p.getPixelStride() +","+p.getRowStride());
            Log.d(TAG, "plan pos/rem," + p.getBuffer().position() +","+ p.getBuffer().remaining());
            totalFromPlanes += p.getBuffer().remaining();
            //Log.d(TAG, "a0:" + p.getBuffer().get());
        }
        Log.d(TAG, "buffer info size/remaining " + bufferInfo.size + "," + buffer.remaining()
        + "\ntotal from planes: " + totalFromPlanes);

        Type type = new Type.Builder(mRs, Element.U8(mRs))
            .setX(totalFromPlanes) // must be same size as buffer we're reading from
            .create();
        Allocation allocYUV = Allocation.createTyped(mRs, type);
        ByteBuffer yuvBuffer = allocYUV.getByteBuffer();
        yuvBuffer.put(planes[0].getBuffer());
        yuvBuffer.put(planes[1].getBuffer());
        yuvBuffer.put(planes[2].getBuffer());

//        Image.Plane yPlane = planes[0];
//        Allocation inputY = initAllocationFromPlane(yPlane);
//        // copy data from buffer into allocation
//        ByteBuffer input = inputY.getByteBuffer();
//        input.put(yPlane.getBuffer());
//
//        Image.Plane uPlane = planes[1];
//        Allocation inputU = initAllocationFromPlane(uPlane);
//        // copy data from buffer into allocation
//        inputU.getByteBuffer().put(uPlane.getBuffer());
//
//        Image.Plane vPlane = planes[2];
//        Allocation inputV = initAllocationFromPlane(uPlane);
//        // copy data from buffer into allocation
//        inputV.getByteBuffer().put(vPlane.getBuffer());

        // create script
        ScriptC_grayscale script = new ScriptC_grayscale(mRs);
        script.set_gInput(allocYUV);
        script.invoke_setInputImageSize(image.getWidth(), image.getHeight());

        // call the script's kernel function for each element in input allocation
        // NOTE: input is a flat buffer of only X in YUV, output is a 2D buffer with X/Y for pixel coordinates in RGB
        script.forEach_root(mRenderAllocation);

        mRs.finish();

        // tell renderAlloc to output to the surface
        mRenderAllocation.ioSend();

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferInfo, bufferIndex, shouldSkip);
    }

    private Allocation createRenderAllocation(Surface surface, int width, int height) {
        // create our output allocation - data from this allocation will be sent to the surface on render
        Type renderType = new Type.Builder(mRs, Element.RGBA_8888(mRs))
            .setX(width)
            .setY(height)
            .create();

        Allocation renderAlloc = Allocation.createTyped(mRs,
                                                        renderType,
                                                        Allocation.USAGE_SCRIPT | Allocation.USAGE_IO_OUTPUT);

        // set the render surface for the output allocation
        renderAlloc.setSurface(surface);
        return renderAlloc;
    }

    private Allocation initAllocationFromPlane(Image.Plane plane) {
        // create our input allocation
        Type type = new Type.Builder(mRs, Element.U8_3(mRs))
            .setX(plane.getBuffer().remaining()) // must be same size as buffer we're reading from
            .create();
        Allocation alloc = Allocation.createTyped(mRs, type);
        return alloc;
    }


}
