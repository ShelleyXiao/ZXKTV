package com.zxktv.opengles;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

import com.zxktv.ZXPlayer.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class DirectDrawer {

    private Context context;
    private FloatBuffer vertexBuffer;
    private final float[] vertexData = {
            1f, 1f, 0f,
            -1f, 1f, 0f,
            1f, -1f, 0f,
            -1f, -1f, 0f
    };

    private FloatBuffer textureBuffer;
    private final float[] textureVertexData = {
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
    };

    /**
     * mediacodec
     */

    private int programId_mediacodec;
    private int aPositionHandle_mediacodec;
    private int textureid_mediacodec;
    private int uTextureSamplerHandle_mediacodec;
    private int aTextureCoordHandle_mediacodec;

    private SurfaceTexture surfaceTexture;
    private Surface surface;



    public DirectDrawer(Context context) {

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureBuffer.position(0);

        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.vertex_base);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.fragment_mediacodec);
        programId_mediacodec = ShaderUtils.createProgram(vertexShader, fragmentShader);

        aPositionHandle_mediacodec = GLES20.glGetAttribLocation(programId_mediacodec, "av_Position");
        aTextureCoordHandle_mediacodec = GLES20.glGetAttribLocation(programId_mediacodec, "af_Position");
        uTextureSamplerHandle_mediacodec = GLES20.glGetUniformLocation(programId_mediacodec, "sTexture");

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureid_mediacodec = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureid_mediacodec);
        ShaderUtils.checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
    }

    public void draw(float[] mtx) {
        GLES20.glUseProgram(programId_mediacodec);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle_mediacodec);
        GLES20.glVertexAttribPointer(aPositionHandle_mediacodec, 3, GLES20.GL_FLOAT, false,
                12, vertexBuffer);

        textureBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle_mediacodec);
        GLES20.glVertexAttribPointer(aTextureCoordHandle_mediacodec, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureid_mediacodec);

        GLES20.glUniform1i(uTextureSamplerHandle_mediacodec, 0);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(aPositionHandle_mediacodec);
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle_mediacodec);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
    }

    public int getTextureid_mediacodec() {
        return textureid_mediacodec;
    }



}
