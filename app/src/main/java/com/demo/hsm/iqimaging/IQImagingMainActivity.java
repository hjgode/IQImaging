package com.demo.hsm.iqimaging;

import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerNotClaimedException;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.Signature;
import com.honeywell.aidc.SignatureParameters;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public class IQImagingMainActivity extends AppCompatActivity implements View.OnClickListener, BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {

    AidcManager mManager;
    BarcodeReader mReader;
    String TAG="IQImagingDemo";

    boolean triggerState=false;
    private boolean isKeyDown = true;

    private Button mClearBtn;
    private Button mCaptureBtn;

    private String mBarcodeStr;
    TextView mBarcodeText;
    ImageView mImageView;

    private boolean isDecoderSuc;
    private boolean isCaptureIQSuc;

    private SignatureParameters mParameters;

    private final int SCANKEY = 292;
    /*
    Image dimensions always a multiple of 8
    - X-dimension (density of the barcode)
    - Ratio (symbol height and width ratio), So the Aspect ratio in a 1D barcode is how many time the width of the narrow bar goes into the height of the barcode.
    - Offset X & Offset Y (position related to the area of interest)
    - Area size to capture (dimension of the area of interest)
    For area size the values are measured in inches then divided by the x-dimension of the symbol
    Sample: for a 20mil symbol, and 1.5 inch height, the value for height should be 1.5/0.02 = 75
    As the image size must always be a multiple of 8, in this case either 72 or 80 could be used.
    */
    SignatureParameters[] sigParamaters=new SignatureParameters[]{
            new SignatureParameters(23, -5, -58,180,90,4,true), //Postal
//            new SignatureParameters(0, 0, 88,440,136,4,true),   //Custom
            new SignatureParameters(45, 0, 80,320,88,4,true),   //Custom
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iqimaging_main);

        mImageView=(ImageView)findViewById(R.id.imageView);
        mBarcodeText=(TextView) findViewById(R.id.txtBarcode);

        mCaptureBtn=(Button)findViewById(R.id.btnScan);
        mClearBtn=(Button)findViewById(R.id.btnClear);

        mCaptureBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);

//        mIQImageView=this.findViewById(R.id.imageView);

        // Create the AidcManager providing a Context and an
        // CreatedCallback implementation.
        AidcManager.create(this, new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {
                mManager = aidcManager;
                // use the manager to create a BarcodeReader with a session
                // associated with the internal imager.
                mReader = mManager.createBarcodeReader();
                if (mReader != null) {
                    Log.d(TAG, "BarcodeReader is onCreated");
                    try {
                        // set the trigger mode to client control
                        mReader.setProperty(
                                BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                                BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);
                        mReader.setProperty(
                                BarcodeReader.PROPERTY_DATA_PROCESSOR_SCAN_TO_INTENT,
                                false);
                        mReader.setProperty(
                                BarcodeReader.PROPERTY_DATA_PROCESSOR_LAUNCH_BROWSER,
                                false);
                        mReader.setProperty(
                                BarcodeReader.PROPERTY_DATA_PROCESSOR_LAUNCH_EZ_CONFIG,
                                false);
                    } catch (UnsupportedPropertyException e) {
                        showToast("Failed to apply properties");
                    }

                    setIQType(Const.IQTYPE_CUSTOM);

                    // register bar code event listener
                    mReader.addBarcodeListener(IQImagingMainActivity.this);
                    // register trigger state change listener
                    mReader.addTriggerListener(IQImagingMainActivity.this);

                    try {
                        // claim the scanner to gain full control
                        mReader.claim();
                    } catch (ScannerUnavailableException e) {
                        e.printStackTrace();
                        showToast("Failed to claim scanner");
                    }

                }

            }
        });

    }

    /*
    set Signature Parameters and Barcode Parameters
     */
    void setIQType(int iqType){
        mParameters=sigParamaters[iqType];
        switch (iqType) {
            case Const.IQTYPE_POSTAL:
                try {
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_128_MAXIMUM_LENGTH, 16);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_128_MINIMUM_LENGTH, 16);

                    mReader.setProperty(BarcodeReader.PROPERTY_AZTEC_ENABLED, false);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_39_ENABLED, false);
                    mReader.setProperty(BarcodeReader.PROPERTY_PDF_417_ENABLED, false);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODABAR_ENABLED, false);
                } catch (UnsupportedPropertyException e) {
                    e.printStackTrace();
                }
                break;
            case Const.IQTYPE_CUSTOM:
                try {
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_128_MAXIMUM_LENGTH, 15);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_128_MINIMUM_LENGTH, 15);

                    mReader.setProperty(BarcodeReader.PROPERTY_AZTEC_ENABLED, false);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODE_39_ENABLED, false);
                    mReader.setProperty(BarcodeReader.PROPERTY_PDF_417_ENABLED, false);
                    mReader.setProperty(BarcodeReader.PROPERTY_CODABAR_ENABLED, false);
                } catch (UnsupportedPropertyException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
//        loadSetting();
        if (mReader != null) {
            try {
                // claim the scanner to gain full control
                mReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                showToast("Failed to claim scanner");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        saveSettings();
        if (mReader != null) {
            mReader.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // make sure that AidcManager isn't null
        if (mReader != null) {
            // unregister barcode event listener
            mReader.removeBarcodeListener(this);
            // unregister trigger state change listener
            mReader.removeTriggerListener(this);
            // close BarcodeReader to clean up resources.
            // once closed, the object can no longer be used.
            mReader.close();
        }
        if (mManager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            mManager.close();
        }

        System.gc();
    }


    private void clearView() {
        if (mBarcodeText != null)
            mBarcodeText.setText("");
        if (mImageView != null) {
            mImageView.setImageBitmap(null);
            mImageView.invalidate();
        }

        isDecoderSuc = false;
        isCaptureIQSuc = false;
        mBarcodeStr = null;
    }

    private void doScan(boolean doScan) {
        if (doScan) {
            clearView();
        }
        try {
            mReader.aim(doScan);
            mReader.light(doScan);
            mReader.decode(doScan);
        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown");
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case SCANKEY:
                if (isKeyDown) {
                    isKeyDown = false;
                    doScan(true);
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                this.finish();
                return true;
            case KeyEvent.KEYCODE_UNKNOWN:
                if (event.getScanCode() == 0x94 || event.getScanCode() == 87
                        || event.getScanCode() == 88) {
                    if (isKeyDown) {
                        isKeyDown = false;
                        doScan(true);
                    }
                    return true;
                }
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp");
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case SCANKEY:
                isKeyDown = true;
                doScan(false);
                return true;
            case 92:
                isKeyDown = true;
                return true;
//            case KeyEvent.KEYCODE_G:
//                okayToSaveRaw = true;
//                return true;
//            case KeyEvent.KEYCODE_J:
//                okayToSavePng = true;
//                return true;
            case KeyEvent.KEYCODE_BACK:
                this.finish();
                return true;
            case KeyEvent.KEYCODE_UNKNOWN: {
                if (event.getScanCode() == 0x94 || event.getScanCode() == 87
                        || event.getScanCode() == 88) {
                    isKeyDown = true;
                    doScan(false);
                }
                return true;
            }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void parseData(BarcodeReadEvent event) {
        Log.d(TAG, "parseData");
        isCaptureIQSuc = false;
        mBarcodeStr = event.getBarcodeData();
        Log.d(TAG, "Code name=" + event.getCodeId());
        if (mBarcodeStr.length() > 0) {
            isDecoderSuc = true;
//            configureProperties();
/*
            E/AndroidRuntime: FATAL EXCEPTION: main
            Process: com.demo.hsm.iqimaging, PID: 21563
            java.lang.RuntimeException: An error occurred while communicating with the scanner service.
                    at com.honeywell.aidc.BarcodeReader.execute(BarcodeReader.java:3000)
            at com.honeywell.aidc.BarcodeReader.getSignature(BarcodeReader.java:2540)
            at com.demo.hsm.iqimaging.IQImagingMainActivity.parseData(IQImagingMainActivity.java:334)
            at com.demo.hsm.iqimaging.IQImagingMainActivity.access$000(IQImagingMainActivity.java:26)
            at com.demo.hsm.iqimaging.IQImagingMainActivity$2.run(IQImagingMainActivity.java:366)
            at android.os.Handler.handleCallback(Handler.java:739)
            at android.os.Handler.dispatchMessage(Handler.java:95)
            at android.os.Looper.loop(Looper.java:148)
            at android.app.ActivityThread.main(ActivityThread.java:5417)
            at java.lang.reflect.Method.invoke(Native Method)
            at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726)
            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)
            Caused by: android.os.DeadObjectException
            at android.os.BinderProxy.transactNative(Native Method)
            at android.os.BinderProxy.transact(Binder.java:503)
            at com.honeywell.IExecutor$Stub$Proxy.execute(IExecutor.java:123)
            at com.honeywell.aidc.BarcodeReader.execute(BarcodeReader.java:2998)
            at com.honeywell.aidc.BarcodeReader.getSignature(BarcodeReader.java:2540) 
            at com.demo.hsm.iqimaging.IQImagingMainActivity.parseData(IQImagingMainActivity.java:334) 
            at com.demo.hsm.iqimaging.IQImagingMainActivity.access$000(IQImagingMainActivity.java:26) 
            at com.demo.hsm.iqimaging.IQImagingMainActivity$2.run(IQImagingMainActivity.java:366) 
            at android.os.Handler.handleCallback(Handler.java:739) 
            at android.os.Handler.dispatchMessage(Handler.java:95) 
            at android.os.Looper.loop(Looper.java:148) 
            at android.app.ActivityThread.main(ActivityThread.java:5417) 
            at java.lang.reflect.Method.invoke(Native Method) 
            at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726) 
            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616) 
            Disconnected from the target VM, address: 'localhost:8600', transport: 'socket'
*/
            if (mImageView != null) {
                try {
                    Signature capture = mReader.getSignature(mParameters);
                    if (capture.getImage() != null) {

                        // release first, than change to updated bitmap
                        //mImageView.releaseBitmap();
                        mImageView.setImageBitmap(capture.getImage());
                        Log.d(TAG, "Get IQ image successfully");
                        isCaptureIQSuc = true;
                        //mImageView.setBitmapFlag(true);
                        mImageView.invalidate();
                    } else {
                        showToast("get_image_fail");
                        clearView();
                    }
                } catch (ScannerUnavailableException e) {
                    showToast("ScannerUnavailableException");
                    e.printStackTrace();
                } catch (ScannerNotClaimedException e) {
                    showToast("ScannerNotClaimedException");
                    e.printStackTrace();
                }
                catch (Exception e){
                    showToast(e.getMessage());
                    e.printStackTrace();
                }
            } else
                Log.d(TAG, "Init IQImage error");

        }
    }

    String getHexedString(String data) {
        StringBuilder s = new StringBuilder();
        for (char b : data.toCharArray()) {
            if(0<b & b<32)
                s.append(String.format("<%2x>", b));
            else
                s.append(b);
        }
        return s.toString();
    }
    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {

        Log.d(TAG, "onBarcodeEvent: "+getHexedString(event.getBarcodeData()));
        // Retrieves a list of 4 points representing the polygon that approximates the boundary of the bar code.
        // The first point is the upper left corner of the bar code, then upper right, bottom, right and finally bottom left.
//points= [Point(382, 425), Point(645, 419), Point(651, 493), Point(383, 498)]
        Point[] boundsList=event.getBarcodeBounds().toArray(new Point[event.getBarcodeBounds().size()]);
        Log.d(TAG, "onBarcodeEvent: points= "+ event.getBarcodeBounds().toString());
        Rect rect=new Rect(boundsList[0].x, boundsList[0].y, boundsList[3].x, boundsList[3].y);
        Log.d(TAG, "onBarcodeEvent: bounds= "+rect.width()+"x"+rect.height());
        //calculate barcode dimension
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                parseData(event);
            }
        });
    }

    @Override
    public void onFailureEvent(final BarcodeFailureEvent event) {

    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent event) {
        try {
            // only handle trigger presses
            //if (event.getState())
            {
                // turn on/off aimer, illumination and decoding
                mReader.aim(!triggerState);
                mReader.light(!triggerState);
                mReader.decode(!triggerState);
                triggerState = !triggerState;
            }

        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
            Toast.makeText(this, "Scanner is not claimed", Toast.LENGTH_SHORT)
                    .show();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
        }

    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        switch (view.getId()) {
            case R.id.btnClear:
                clearView();
                break;
            case R.id.btnScan:
                doScan(true);
                break;
            default:
                break;
        }
    }

}
