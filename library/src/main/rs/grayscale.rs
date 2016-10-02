#pragma version(1)

#pragma rs java_package_name(com.google.android.exoplayer)

rs_allocation gInput;
//rs_allocation gInputU;
//rs_allocation gInputV;
rs_allocation gRenderAllocation; // 2D array of RGB pixels

int wIn, hIn;
int numTotalPixels;

// Function to invoke before applying conversion
void setInputImageSize(int _w, int _h)
{
    wIn = _w;
    hIn = _h;
    numTotalPixels = wIn * hIn;
}

const static float3 grayMultipliers = {0.299f, 0.587f, 0.114f};

// Kernel that converts a YUV element to a RGBA one
uchar4 RS_KERNEL root(uint32_t x, uint32_t y)
{

    // YUV 4:2:0 planar image, with 8 bit Y samples, followed by
    // interleaved V/U plane with 8bit 2x2 subsampled chroma samples
    int baseIdx = x + y * wIn;
    int baseUYIndex = numTotalPixels + (y >> 1) * wIn + (x & 0xfffffe);

    uchar _y = rsGetElementAt_uchar(gInput, baseIdx);
    uchar _u = rsGetElementAt_uchar(gInput, baseUYIndex);
    uchar _v = rsGetElementAt_uchar(gInput, baseUYIndex + 1);
    _y = _y < 16 ? 16 : _y;

    short Y = ((short)_y) - 16;
    short U = ((short)_u) - 128;
    short V = ((short)_v) - 128;

    uchar4 out;
    out.r = (uchar) clamp((float)(
        (Y * 298 + V * 409 + 128) >> 8), 0, 255);
    out.g = (uchar) clamp((float)(
        (Y * 298 - U * 100 - V * 208 + 128) >> 8), 0, 255);
    out.b = (uchar) clamp((float)(
        (Y * 298 + U * 516 + 128) >> 8), 0, 255);
    out.a = 255;

    // Multiplies each input's pixel RGB values by their respective gray multipliers
    uchar grayValue = (uchar) ((float) out.r * grayMultipliers.r +
                      (float) out.g * grayMultipliers.g +
                      (float) out.b * grayMultipliers.b);

    uchar4 pixelOut;
    pixelOut.r = grayValue;
    pixelOut.g = grayValue;
    pixelOut.b = grayValue;
    pixelOut.a = out.a; // Preserve alpha

    return pixelOut;
}
