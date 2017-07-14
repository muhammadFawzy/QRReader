package com.f4wzy.qrorientation;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * qrcode orientation example
 */
public class MainActivity extends Activity implements CvCameraViewListener2 {

    /**
     * class name for debugging with logcat
     */
    private static final String TAG = MainActivity.class.getName();

    /**
     * frame size width
     */
    private static final int FRAME_SIZE_WIDTH = 640;
    /**
     * frame size height
     */
    private static final int FRAME_SIZE_HEIGHT = 480;
    /**
     * runtime camera permisson
     */

    private static final int CAMERA_PERMISSION = 100;
    /**
     * whether or not to use a fixed frame size -> results usually in higher FPS
     * 640 x 480
     */
    private static final boolean FIXED_FRAME_SIZE = true;

    /**
     * text view for direction
     */
    TextView tv_orientation;
    /**
     * the camera image
     */
    Mat img;
    /**
     * the template image used for template matching
     * or for copying into the camera view
     */

    /**
     * the camera view
     */
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");


                    img = new Mat();

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        tv_orientation = (TextView) findViewById(R.id.tv_orientation);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        // Michael Troger
        if (FIXED_FRAME_SIZE) {
            mOpenCvCameraView.setMaxFrameSize(FRAME_SIZE_WIDTH, FRAME_SIZE_HEIGHT);
        }


        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);

            return;
        }

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        img = inputFrame.gray();


        try {
            zxing(img);
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }


        return img;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mOpenCvCameraView.setCvCameraViewListener(this);
            } else Toast.makeText(this, "permission denied!", Toast.LENGTH_LONG).show();
        }

    }

    public void zxing(Mat mRgba) throws ChecksumException, FormatException {

        Bitmap bMap = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.ARGB_8888);
        Log.e("bitmap", bMap.getHeight() + " " + bMap.getWidth());
        Log.e("mat", mRgba.rows() + " " + mRgba.cols());
        Utils.matToBitmap(mRgba, bMap);
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new QRCodeReader();


        try {

            Result result = null;
            try {
                result = reader.decode(bitmap);


                if (result.getText() != null) {

                    ResultPoint[] points = result.getResultPoints();
                    double dst1 = Math.sqrt((Math.pow(points[0].getX(), 2) + Math.pow(points[0].getY(), 2)));
                    double dst2 = Math.sqrt((Math.pow(points[1].getX(), 2) + Math.pow(points[1].getY(), 2)));
                    double dst3 = Math.sqrt((Math.pow(points[2].getX(), 2) + Math.pow(points[2].getY(), 2)));

                    ResultPoint point = new ResultPoint(points[2].getX(), points[0].getY());
                    for (int i = 0; i < points.length; i++) {

                        if (points[i] == point) {
                            point = new ResultPoint(points[0].getX(), points[2].getY());
                            break;
                        }
                    }

                    double dst4 = Math.sqrt((Math.pow(point.getX(), 2) + Math.pow(point.getY(), 2)));
                    LinkedList<Double> unsorted = new LinkedList<>();
                    unsorted.add(dst1);
                    unsorted.add(dst2);
                    unsorted.add(dst3);
                    unsorted.add(dst4);
                    List<Double> dsts = new ArrayList<>();
                    dsts.add(dst1);
                    dsts.add(dst2);
                    dsts.add(dst3);
                    dsts.add(dst4);


                    Collections.sort(dsts);


                    int index = unsorted.indexOf(dsts.get(0));


                    String rs = "";

                    if (index == 0)
                        rs = "D";
                    else if (index == 1)
                        rs = "A";
                    else if (index == 2)
                        rs = "B";
                    else if (index == 3)
                        rs = "C";


                    // 1- point


                    final Result finalResult = result;
                    final String finalRs = rs;
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_orientation.setText(finalResult.getText() + ":" + finalRs);
                        }
                    });
                }


                //convert it to mat


            } catch (com.google.zxing.FormatException e) {
                e.printStackTrace();
            }


        } catch (NotFoundException e) {
            //  Log.d(TAG, "Code Not Found");
            e.printStackTrace();
        }
    }


}
