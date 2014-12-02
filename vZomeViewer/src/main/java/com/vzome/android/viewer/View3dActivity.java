/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vzome.android.viewer;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.vrtoolkit.cardboard.*;

import javax.microedition.khronos.egl.EGLConfig;

import java.io.InputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.vzome.api.Ball;
import com.vzome.api.Application;
import com.vzome.api.Document;
import com.vzome.api.Strut;

import org.vorthmann.zome.math.Polyhedron;
import org.vorthmann.zome.model.real.Connector;
import org.vorthmann.zome.model.real.Manifestation;
import org.vorthmann.zome.render.Color;
import org.vorthmann.zome.render.Colors;
import org.vorthmann.zome.render.RenderedManifestation;
import org.vorthmann.zome.render.RenderedModel;

/**
 * A Cardboard sample application.
 */
public class View3dActivity extends CardboardActivity implements CardboardView.StereoRenderer
{
    private static final String TAG = "View3dActivity";

    private static final float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.1f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    private final WorldLayoutData DATA = new WorldLayoutData();

    private RenderingProgram instancedRenderer, floorRenderer, lineRenderer, experimentalRenderer;
    private Set<ShapeClass> shapes = new HashSet<ShapeClass>();
    private boolean buffersCopied = false;
    private boolean loading = true;
    private boolean failedLoad = false;
    private boolean experimental = false;
    private float[][] orientations;

    private void addShapeClass( Polyhedron shape, ShapeClass.Config config )
    {
        ShapeClass shapeClass = ShapeClass .create( shape, config.instances, config.color );
        shapes .add( shapeClass );
        Log.i(TAG, "%%%%%%%%%%%%%%%% new shapeClass");
    }

    private ShapeClass mFloor, struts;

    private float[] mModelCube;
    private float[] mCamera;
    private float[] mHeadView;

    private float[] mModelFloor;

    private float mObjectDistance = 1f;
    private float mFloorDepth = 20f;

    private Vibrator mVibrator;

    private CardboardOverlayView mOverlayView;

    private Application vZome;

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     * @param savedInstanceState
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView .setRenderer( this );

        float fov = cardboardView .getZFar();
        cardboardView .setFovY( 15f );
        cardboardView .setZPlanes( 0.1f, 200f );
        cardboardView .setDistortionCorrectionEnabled( false );

        setCardboardView( cardboardView );

        mModelCube = new float[16];
        mCamera = new float[16];
        mModelFloor = new float[16];
        mHeadView = new float[16];
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Intent intent = getIntent();
        String modelUrl = intent .getStringExtra( MainActivity.EXTRA_MESSAGE );

        mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        mOverlayView.show3DToast( "Loading the vZome model.\nThis may take several seconds." );

        vZome = new Application();

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadVzomeTask().execute( modelUrl );
        } else {
//            textView.setText("No network connection available.");
        }
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    /**
     * Creates the buffers we use to store information about the 3D world. OpenGL doesn't use Java
     * arrays, but rather needs data in a format it can understand. Hence we use ByteBuffers.
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");

        mFloor = new ShapeClass( DATA.FLOOR_COORDS, DATA.FLOOR_NORMALS, null, DATA.FLOOR_COLOR );

        this .lineRenderer = new RenderingProgram( getResources(), false, false, false );

        this .floorRenderer = new RenderingProgram( getResources(), true, false, false );

        this .instancedRenderer = new RenderingProgram( getResources(), true, true, false );

        this .experimentalRenderer = new RenderingProgram( getResources(), true, true, true );

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        // Object first appears directly in front of user
        Matrix.setIdentityM(mModelCube, 0);
        Matrix.translateM(mModelCube, 0, 0, 0, -mObjectDistance);

        Matrix.setIdentityM(mModelFloor, 0);
        Matrix.translateM(mModelFloor, 0, 0, -mFloorDepth, 0); // Floor appears below user
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame( HeadTransform headTransform )
    {
        // Build the Model part of the ModelView matrix.
//        Matrix.rotateM(mModelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

        headTransform.getHeadView(mHeadView, 0);

        // an attempt at orbit mode... does not work at all
        float[] inv = new float[16];
        Matrix.invertM( inv, 0, mHeadView, 0 );
        float[] camStart = new float[]{ 0f, 0f, CAMERA_Z, 0f };
        float[] camEnd = new float[4];
        Matrix .multiplyMV( camEnd, 0, inv, 0, camStart , 0 );
        float[] upStart = new float[]{ 0.0f, 1.0f, 0.0f, 0f };
        float[] upEnd = new float[4];
        Matrix .multiplyMV( upEnd, 0, inv, 0, upStart , 0 );
        Matrix.setLookAtM( mCamera, 0, camEnd[0], camEnd[1], camEnd[2], 0.0f, 0.0f, 0.0f, upEnd[0], upEnd[1], upEnd[2] );

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM( mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f );

        if ( !buffersCopied && !loading && !failedLoad ) {
            Log.i( TAG, "creating VBOs" );
            // First, generate as many buffers as we need.
            // This will give us the OpenGL handles for these buffers.
            int numBuffers = 3 * this.shapes.size();
            final int buffers[] = new int[ numBuffers ];
            GLES30.glGenBuffers( numBuffers, buffers, 0 );

            int bufferTriple = 0;
            for( ShapeClass shape : shapes )
            {
                // Bind to the buffer. glBufferData will affect this buffer specifically.
                GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, buffers[ bufferTriple + 0 ] );
                // Transfer data from client memory to the buffer.
                // We can release the client memory after this call.
                FloatBuffer clientBuffer = shape .getVertices();
                GLES30.glBufferData( GLES30.GL_ARRAY_BUFFER, clientBuffer .capacity() * 4,
                        clientBuffer, GLES30.GL_STATIC_DRAW );
                // IMPORTANT: Unbind from the buffer when we're done with it.
                GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, 0 );

                // Bind to the buffer. glBufferData will affect this buffer specifically.
                GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, buffers[ bufferTriple + 1 ] );
                // Transfer data from client memory to the buffer.
                // We can release the client memory after this call.
                clientBuffer = shape .getNormals();
                GLES30.glBufferData( GLES30.GL_ARRAY_BUFFER, clientBuffer .capacity() * 4,
                        clientBuffer, GLES30.GL_STATIC_DRAW );
                // IMPORTANT: Unbind from the buffer when we're done with it.
                GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, 0 );

                // Bind to the buffer. glBufferData will affect this buffer specifically.
                GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, buffers[ bufferTriple + 2 ] );
                // Transfer data from client memory to the buffer.
                // We can release the client memory after this call.
                clientBuffer = shape .getPositions();
                GLES30.glBufferData( GLES30.GL_ARRAY_BUFFER, clientBuffer .capacity() * 4,
                        clientBuffer, GLES30.GL_STATIC_DRAW );
                // IMPORTANT: Unbind from the buffer when we're done with it.
                GLES30.glBindBuffer( GLES30.GL_ARRAY_BUFFER, 0 );

                shape .setBuffers( buffers[ bufferTriple + 0 ], buffers[ bufferTriple + 1 ], buffers[ bufferTriple + 2 ] );
                bufferTriple += 3;
                Log.i( TAG, "another buffer triple loaded into GPU" );
            }
            RenderingProgram .checkGLError( "loadVBOs" );
            buffersCopied = true;
        }
    }

    /**
     * Draws a frame for an eye. The transformation for that eye (from the camera) is passed in as
     * a parameter.
     * @param transform The transformations to apply to render this eye.
     */
    @Override
    public void onDrawEye(EyeTransform transform)
    {
        if ( failedLoad )
            GLES30.glClearColor( 0.5f, 0f, 0f, 1f );
        else if ( loading )
            GLES30.glClearColor( 0.2f, 0.3f, 0.4f, 0.5f );
        else
            GLES30.glClearColor( 0.5f, 0.6f, 0.7f, 0.5f );

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        RenderingProgram .checkGLError("glClear");

        if ( buffersCopied )
        {
            if ( this .experimental )
            {
                this.experimentalRenderer.setUniforms( mModelCube, mCamera, transform, orientations );
                for( ShapeClass shapeClass : shapes )
                    this.experimentalRenderer.renderShape( shapeClass );
            }
            else {
                this.instancedRenderer.setUniforms( mModelCube, mCamera, transform, orientations );
                for( ShapeClass shapeClass : shapes )
                    this.instancedRenderer.renderShape( shapeClass );
            }
        }
        else
        {
            if (struts != null) {
                this.lineRenderer.setUniforms( mModelCube, mCamera, transform, orientations );
                this.lineRenderer.renderShape( struts );
            }
        }

        this .floorRenderer .setUniforms( mModelFloor, mCamera, transform, orientations );
        this .floorRenderer .renderShape( mFloor );
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    @Override
    public boolean dispatchGenericMotionEvent( MotionEvent me )
    {
        return super .dispatchGenericMotionEvent( me );
    }

    @Override
    public boolean dispatchKeyEvent( KeyEvent ke )
    {
        int code = ke .getKeyCode();
        Log.i( TAG, "key code is : " + code );
        if ( code == KeyEvent.KEYCODE_BUTTON_R2 ) {
            mOverlayView.show3DToast( "Using EXPERIMENTAL rendering!" );
            this .experimental = true;
            return true;
        }
        else if ( code == KeyEvent.KEYCODE_BUTTON_L2 ) {
            mOverlayView.show3DToast( "Using default rendering." );
            this .experimental = false;
            return true;
        }
        else if ( code == KeyEvent.KEYCODE_BUTTON_X && ke .getAction() == KeyEvent.ACTION_DOWN ) {
            hideObject();
            mVibrator .vibrate(50);
            return true;
        }
        else
            return super .dispatchKeyEvent( ke );
    }

    /**
     * Increment the score, hide the object, and give feedback if the user pulls the magnet while
     * looking at the object. Otherwise, remind the user what to do.
     */
    @Override
    public void onCardboardTrigger()
    {
        Log.i( TAG, "onCardboardTrigger" );

        hideObject();
        // Always give user feedback

        mVibrator .vibrate(50);
    }

    /**
     * Find a new random position for the object.
     * We'll rotate it around the Y-axis so it's out of sight, and then up or down by a little bit.
     */
    private void hideObject() {
        float[] rotationMatrix = new float[16];
        float[] posVec = new float[4];

        // First rotate in XZ plane, between 90 and 270 deg away, and scale so that we vary
        // the object's distance from the user.

        // SV: I'm only adjusting distance now.

        float angleXZ = (float) 0; // Math.random() * 180 + 90;
        Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);
        float oldObjectDistance = mObjectDistance;
        mObjectDistance = (float) Math.random() * 30 + 40;
        float objectScalingFactor = mObjectDistance / oldObjectDistance;
        Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor, objectScalingFactor);
        Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, mModelCube, 12);

        // Now get the up or down angle, between -20 and 20 degrees
        float angleY = 0f; // (float) Math.random() * 80 - 40; // angle in Y plane, between -40 and 40
        angleY = (float) Math.toRadians(angleY);
        float newY = (float)Math.tan(angleY) * mObjectDistance;

        Matrix.setIdentityM(mModelCube, 0);
        Matrix.translateM(mModelCube, 0, posVec[0], newY, posVec[2]);
    }

    /**
     * Check if user is looking at object by calculating where the object is in eye-space.
     * @return
     */
    private boolean isLookingAtObject() {
        float[] initVec = {0, 0, 0, 1.0f};
        float[] objPositionVec = new float[4];
        float[] mModelView = new float[16];

        // Convert object space to camera space. Use the headView from onNewFrame.
        Matrix.multiplyMM(mModelView, 0, mHeadView, 0, mModelCube, 0);
        Matrix.multiplyMV(objPositionVec, 0, mModelView, 0, initVec, 0);

        float pitch = (float)Math.atan2(objPositionVec[1], -objPositionVec[2]);
        float yaw = (float)Math.atan2(objPositionVec[0], -objPositionVec[2]);

        Log.v(TAG, "Object position: X: " + objPositionVec[0]
                + "  Y: " + objPositionVec[1] + " Z: " + objPositionVec[2]);
        Log.v(TAG, "Object Pitch: " + pitch +"  Yaw: " + yaw);

        return (Math.abs(pitch) < PITCH_LIMIT) && (Math.abs(yaw) < YAW_LIMIT);
    }


    // Uses AsyncTask to create a task away from the main UI thread.
    private class DownloadVzomeTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... urls) {{

            // params comes from the execute() call: params[0] is the url.
            try {
                URL url = new URL( urls[ 0 ] );
                Log.i(TAG, "%%%%%%%%%%%%%%%% opening: " + url);
                InputStream instream = url.openStream();
                Document doc = vZome.loadDocument(instream);
                instream.close();
                Log.i( TAG, "%%%%%%%%%%%%%%%% finished: " + url );

                Colors colors = vZome .getColors();
                View3dActivity.this.orientations = doc .getOrientations();

//                float[] white = new float[] { 1f, 1f, 1f, 1f };
//                View3dActivity.this.balls = ShapeClass .create( vZome .getBallShape(), doc .getBalls(), white );

                float[] black = new float[] { 0f, 0f, 0f, 1f };
                View3dActivity.this.struts = ShapeClass .create( doc .getStruts().toArray(), black );

                Map<Polyhedron,ShapeClass.Config> shapeClasses = new HashMap<Polyhedron,ShapeClass.Config>();
                RenderedModel rmodel = doc .getRenderedModel();
                Iterator rms = rmodel .getRenderedManifestations();
                while ( rms .hasNext() ) {
                    RenderedManifestation rman = (RenderedManifestation) rms .next();

                    Polyhedron shape = rman .getShape();
                    ShapeClass.Config scc = shapeClasses .get( shape );
                    if ( scc == null ) {
                        Log.i( TAG, "%%%%%%%%%%%%%%%% new shape" );
                        scc = new ShapeClass.Config();
                        shapeClasses .put( shape, scc );
                        Color color = colors .getColor( rman .getColorName() );
                        float[] rgb = new float[3];
                        color .getRGBColorComponents( rgb );
                        scc .color = new float[]{ rgb[0], rgb[1], rgb[2], 1f };
                    }

                    Manifestation man = rman.getManifestation();
                    Object instance = null;
                    if ( man instanceof Connector) {
                        instance = new Ball( rmodel.getField(), (Connector) man );
                    }
                    else if ( man instanceof org.vorthmann.zome.model.real.Strut ) {
                        int zone = rman .getStrutZone();

                        instance = new Strut( rmodel.getField(), (org.vorthmann.zome.model.real.Strut) man, zone );
                    }
                    else {
                        Log.w( TAG, "%%%%%%%%%%%%%%%% missing panel!" );
                        continue;
                    }
                    scc .instances .add( instance );
                }
                for( Map.Entry<Polyhedron, ShapeClass.Config> entry : shapeClasses.entrySet() )
                {
                    View3dActivity.this.addShapeClass( entry .getKey(), entry .getValue() );
                }
                View3dActivity.this.loading = false;
            }
            catch (Exception e) {
                View3dActivity.this.failedLoad = true;
                Log.e( TAG, "%%%%%%%%%%%%%%%% FAILED: " + urls[ 0 ] );
                e .printStackTrace();
            }
            return "OK";
        }}

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //           textView.setText(result);
        }
    }
}
