package com.ukiuni.slite;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.Menu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends SliteBaseActivity {
    GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.surface);
        glSurfaceView.setRenderer(new MyRenderer());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private class MyRenderer implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glDepthFunc(GL10.GL_LEQUAL);
            new Thread() {
                @Override
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (true) return;
                    if (null != SliteApplication.currentAccount()) {
                        TopActivity.start(MainActivity.this, SliteApplication.currentAccount().id);
                    } else {
                        SigninActivity.start(MainActivity.this);
                    }
                }
            }.start();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            GLU.gluPerspective(gl, 45f, (float) width / height, 1f, 50f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            gl.glClearColor(1, 1, 1, 1);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0, 0, -3f);

            mVertexBuffer.position(0);
            gl.glColor4f(0.1f, 0.3f, 0.6f, 1.0f);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer); // Front
            gl.glNormal3f(0, 0, 1.0f);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 12);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            //0.0f, 0.5f, 0.5f,
            //0.5f, 0.3f, 0.5f,
            if (Float.compare(rad, 100.0f) < 0) {
                mVertexBuffer2.put(0, rad / 100.0f * (0.5f - 0.0f) + 0.0f);
                mVertexBuffer2.put(1, rad / 100.0f * (0.3f - 0.5f) + 0.5f);
            } else {
                mVertexBuffer2.put(0, 0.5f);
                mVertexBuffer2.put(1, 0.3f);
            }
            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer2); // Front
            gl.glNormal3f(0, 0, 1.0f);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

            if (Float.compare(rad, 100.0f) > 0) {
                if (null != SliteApplication.currentAccount()) {
                    TopActivity.start(MainActivity.this, SliteApplication.currentAccount().id);
                } else {
                    SigninActivity.start(MainActivity.this);
                }
                finish();
            } else {
                rad++;
            }
        }
    }

    public float rad = 0.0f;
    public static FloatBuffer mVertexBuffer;
    public static FloatBuffer mVertexBuffer2;

    static {
        float vertices[] = {
                0.0f, -0.5f, 0.5f,
                0.5f, -0.3f, 0.5f,
                0.5f, 0.3f, 0.5f,

                0.0f, -0.5f, 0.5f,
                0.5f, 0.3f, 0.5f,
                0.0f, 0.5f, 0.5f,

                0.0f, -0.5f, 0.5f,
                -0.5f, 0.3f, 0.5f,
                0.0f, 0.5f, 0.5f,

                0.0f, -0.5f, 0.5f,
                -0.5f, -0.3f, 0.5f,
                -0.5f, 0.3f, 0.5f
        };
        float vertices2[] = {
                0.0f, 0.5f, 0.5f,
                0.5f, 0.3f, 0.5f,
                0.0f, 0.0f, 0.5f,
        };
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);
        ByteBuffer vbb2 = ByteBuffer.allocateDirect(vertices2.length * 4);
        vbb2.order(ByteOrder.nativeOrder());
        mVertexBuffer2 = vbb2.asFloatBuffer();
        mVertexBuffer2.put(vertices2);
        mVertexBuffer2.position(0);
    }


}
