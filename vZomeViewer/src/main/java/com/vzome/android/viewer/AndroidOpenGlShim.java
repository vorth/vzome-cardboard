package com.vzome.android.viewer;

import android.opengl.GLES30;
import android.opengl.Matrix;

import com.vzome.opengl.OpenGlShim;

import java.nio.FloatBuffer;

/**
 * Created by vorth on 9/20/15.
 */
public class AndroidOpenGlShim implements OpenGlShim {

    @Override
    public int glCreateProgram() {
        return GLES30.glCreateProgram();
    }

    @Override
    public void glAttachShader(int i, int i2) {
        GLES30.glAttachShader( i, i2 );
    }

    @Override
    public void glLinkProgram(int i) {
        GLES30.glLinkProgram(i);
    }

    @Override
    public int glCreateVertexShader() {
        return GLES30.glCreateShader( GLES30.GL_VERTEX_SHADER );
    }

    @Override
    public int glCreateFragmentShader() {
        return GLES30.glCreateShader( GLES30.GL_FRAGMENT_SHADER );
    }

    @Override
    public void glShaderSource(int i, String s) {
        GLES30.glShaderSource( i, s );
    }

    @Override
    public void glCompileShader(int i) {
        GLES30.glCompileShader(i);
    }

    @Override
    public void glGetShaderStatus(int i, int[] ints, int i2) {
        GLES30.glGetShaderiv(i, GLES30.GL_COMPILE_STATUS, ints, i2);
    }

    @Override
    public String glGetShaderInfoLog(int i) {
        return GLES30.glGetShaderInfoLog( i );
    }

    @Override
    public void glDeleteShader(int i) {
        GLES30.glDeleteShader( i );
    }

    @Override
    public int glGetError() {
        return GLES30.glGetError();
    }

    @Override
    public int glGetUniformLocation(int i, String s) {
        return GLES30.glGetUniformLocation( i, s );
    }

    @Override
    public int glGetAttribLocation(int i, String s) {
        return GLES30.glGetAttribLocation( i, s );
    }

    @Override
    public void glUseProgram(int i) {
        GLES30.glUseProgram( i );
    }

    @Override
    public void glUniformMatrix4fv(int i, int i2, boolean b, float[] floats, int i3) {
        GLES30.glUniformMatrix4fv( i, i2, b, floats, i3 );
    }

    @Override
    public void glUniform3f(int i, float v, float v2, float v3) {
        GLES30.glUniform3f( i, v, v2, v3 );
    }

    @Override
    public void glUniform4f(int i, float v, float v2, float v3, float v4) {
        GLES30.glUniform4f( i, v, v2, v3, v4 );
    }

    @Override
    public void glEnableVertexAttribArray(int i) {
        GLES30.glEnableVertexAttribArray( i );
    }

    @Override
    public void glBindBuffer( int i) {
        GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, i );
    }

    @Override
    public void glVertexAttribDivisor(int i, int i2) {
        GLES30.glVertexAttribDivisor( i, i2 );
    }

    @Override
    public void glVertexAttribPointer(int i, int i2, boolean b, int i3, int i4) {
        GLES30.glVertexAttribPointer( i, i2, GLES30.GL_FLOAT, b, i3, i4 );
    }

    @Override
    public void glVertexAttribPointer(int i, int i2, boolean b, int i3, FloatBuffer floatBuffer) {
        GLES30.glVertexAttribPointer( i, i2, GLES30.GL_FLOAT, b, i3, floatBuffer );
    }

    @Override
    public void glDrawArraysInstanced( int i, int i2, int i3) {
        GLES30.glDrawArraysInstanced( GLES30.GL_TRIANGLES, i, i2, i3 );
    }

    @Override
    public void glDrawTriangles( int i, int i2) {
        GLES30.glDrawArrays( GLES30.GL_TRIANGLES, i, i2 );
    }

    @Override
    public void glDrawLines( int i, int i2) {
        GLES30.glDrawArrays( GLES30.GL_LINES, i, i2 );
    }

    @Override
    public void glGenBuffers(int i, int[] ints, int i2) {
        GLES30.glGenBuffers( i, ints, i2 );
    }

    @Override
    public void glBufferData( int i, FloatBuffer floatBuffer ) {
        GLES30.glBufferData( GLES30.GL_ARRAY_BUFFER, i, floatBuffer, GLES30.GL_STATIC_DRAW );
    }

    @Override
    public void multiplyMM(float[] floats, float[] floats2, float[] floats3) {
        Matrix.multiplyMM( floats, 0, floats2, 0, floats3, 0 );
    }

    @Override
    public void invertM(float[] floats, float[] floats2) {
        Matrix.invertM( floats, 0, floats2, 0 );
    }

    @Override
    public void transposeM(float[] floats, float[] floats2) {
        Matrix.transposeM( floats, 0, floats2, 0 );
    }

    @Override
    public void multiplyMV(float[] floats, float[] floats2, float[] floats3 ) {
        Matrix.multiplyMV( floats, 0, floats2, 0, floats3, 0 );
    }
}
