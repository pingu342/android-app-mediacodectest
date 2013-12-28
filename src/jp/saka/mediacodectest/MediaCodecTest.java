package jp.saka.mediacodectest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodecList;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.graphics.ImageFormat;
import android.view.Window;

import java.nio.ByteBuffer;

public class MediaCodecTest extends Activity
{
	private static final int UseCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
	//private static final int UseCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
	//private static final int UseCamera = -1;
	private static final int Width = 1280;
	private static final int Height = 720;
	private static final int Bitrate = 100000;
	private static final int Framerate = 15;

	private SurfaceView mCameraSurface = null;
	private SurfaceView mDecodeSurface = null;
	private Camera mCamera = null;
	private RingBuffer mRingBuffer = new RingBuffer();
	private EncDecThread mEncDecThread = null;
	private boolean mResumed = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("sakalog", "onCreate");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		// エンコーダ入力映像をカメラから取得するためのカメラプレビュー用SurfaceViewを準備
		mCameraSurface = (SurfaceView)findViewById(R.id.CameraSurface);
		mCameraSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.d("sakalog", "mCameraSurface surfaceChanged");
				
				// カメラをスタート
				if (mResumed) {
					startCamera();
				}
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d("sakalog", "mCameraSurface surfaceCreated");
			}
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.d("sakalog", "mCameraSurface surfaceDestroyed");
			}
		});

		// デコーダ出力映像を描画するSurfaceViewを準備
		mDecodeSurface = (SurfaceView)findViewById(R.id.DecodeSurface);
		mDecodeSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.d("sakalog", "mDecodeSurface surfaceChanged");

				// エンコード・デコードを開始
				if (mEncDecThread != null) {
					mEncDecThread.finish();
				}
				if (mResumed) {
					mEncDecThread = new EncDecThread();
					mEncDecThread.start();
				}
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d("sakalog", "mDecodeSurface surfaceCreated");
			}
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.d("sakalog", "mDecodeSurface surfaceDestroyed");
			}
		});

	}

	@Override
	public void onResume() {
		Log.d("sakalog", "onResume");
		super.onResume();
		mResumed = true;
		if (mCameraSurface.getHolder().getSurface().isValid()) {
			Log.d("sakalog", "mCameraSurface is valid");

			// カメラをスタート
			startCamera();
		} else {
			Log.d("sakalog", "mCameraSurface is invalid");
		}
		if (mDecodeSurface.getHolder().getSurface().isValid()) {
			Log.d("sakalog", "mDecodeSurface is valid");

			// エンコード・デコードを開始
			if (mEncDecThread != null) {
				mEncDecThread.finish();
			}
			mEncDecThread = new EncDecThread();
			mEncDecThread.start();
		} else {
			Log.d("sakalog", "mDecodeSurface is invalid");
		}
	 }

	@Override
	public void onPause() {
		Log.d("sakalog", "onPause");
		super.onPause();
		mResumed = false;

		// エンコード・デコードを終了
		if (mEncDecThread != null) {
			mEncDecThread.finish();
			mEncDecThread = null;
		}

		// カメラを停止
		stopCamera();
	}

	@Override
	public void onDestroy() {
		Log.d("sakalog", "onDestroy");
		super.onDestroy();
	}

	private String colorFormatName(int format) {
		String name;
		switch (format) {
			case MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444:
				name = "COLOR_Format12bitRGB444";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB1555:
				name = "COLOR_Format16bitARGB1555";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB4444:
				name = "COLOR_Format16bitARGB4444";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitBGR565:
				name = "COLOR_Format16bitBGR565";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565:
				name = "COLOR_Format16bitRGB565";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format18BitBGR666:
				name = "COLOR_Format18BitBGR666";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format18bitARGB1665:
				name = "COLOR_Format18bitARGB1665";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format18bitRGB666:
				name = "COLOR_Format18bitRGB666";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format19bitARGB1666:
				name = "COLOR_Format19bitARGB1666";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format24BitABGR6666:
				name = "COLOR_Format24BitABGR6666";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format24BitARGB6666:
				name = "COLOR_Format24BitARGB6666";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format24bitARGB1887:
				name = "COLOR_Format24bitARGB1887";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888:
				name = "COLOR_Format24bitBGR888";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888:
				name = "COLOR_Format24bitRGB888";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format25bitARGB1888:
				name = "COLOR_Format25bitARGB1888";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888:
				name = "COLOR_Format32bitARGB8888";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format32bitBGRA8888:
				name = "COLOR_Format32bitBGRA8888";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_Format8bitRGB332:
				name = "COLOR_Format8bitRGB332";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatCbYCrY:
				name = "COLOR_FormatCbYCrY";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatCrYCbY:
				name = "COLOR_FormatCrYCbY";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatL16:
				name = "COLOR_FormatL16";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatL2:
				name = "COLOR_FormatL2";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatL24:
				name = "COLOR_FormatL24";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatL32:
				name = "COLOR_FormatL32";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatL4:
				name = "COLOR_FormatL4";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatL8:
				name = "COLOR_FormatL8";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatMonochrome:
				name = "COLOR_FormatMonochrome";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer10bit:
				name = "COLOR_FormatRawBayer10bit";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bit:
				name = "COLOR_FormatRawBayer8bit";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bitcompressed:
				name = "COLOR_FormatRawBayer8bitcompressed";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr:
				name = "COLOR_FormatYCbYCr";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYCrYCb:
				name = "COLOR_FormatYCrYCb";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411PackedPlanar:
				name = "COLOR_FormatYUV411PackedPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411Planar:
				name = "COLOR_FormatYUV411Planar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
				name = "COLOR_FormatYUV420PackedPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
				name = "COLOR_FormatYUV420PackedSemiPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
				name = "COLOR_FormatYUV420Planar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
				name = "COLOR_FormatYUV420SemiPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar:
				name = "COLOR_FormatYUV422PackedPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar:
				name = "COLOR_FormatYUV422PackedSemiPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar:
				name = "COLOR_FormatYUV422Planar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar:
				name = "COLOR_FormatYUV422SemiPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Interleaved:
				name = "COLOR_FormatYUV444Interleaved";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
				name = "COLOR_QCOM_FormatYUV420SemiPlanar";
				break;
			case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
				name = "COLOR_TI_FormatYUV420PackedSemiPlanar";
				break;
			default:
				name = "???";
		}
		name += "(" + format + ")";
		return name;
	}

	private void startCamera() {

		if (mCamera != null || mRingBuffer == null) {
			return;
		}

		if (!(UseCamera == Camera.CameraInfo.CAMERA_FACING_FRONT || UseCamera == Camera.CameraInfo.CAMERA_FACING_BACK)) {
			return;
		}

		int facing = UseCamera;
		int width = Width;
		int height = Height;

		try {
			int cameraId = -1;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

			for (int i=0; i<Camera.getNumberOfCameras(); i++) {
				Camera.getCameraInfo(i, cameraInfo);
				if (cameraInfo.facing == facing) {
					cameraId = i;
					break;
				}
			}

			if (cameraId == -1) {
				Log.d("sakalog", "Camera not found.");
				return;
			}

			Camera camera = Camera.open(cameraId); 

			Parameters params = camera.getParameters();
			params.setPreviewSize(width, height); 
			//params.setPreviewFpsRange(chosenFps[0], chosenFps[1]);
			camera.setParameters(params);

			Log.d("sakalog", "Camera preview format " + params.getPreviewFormat());
			Log.d("sakalog", "Bits per pixel " + ImageFormat.getBitsPerPixel(params.getPreviewFormat()));

			int bufferSize = (width * height * ImageFormat.getBitsPerPixel(params.getPreviewFormat())) / 8;
			camera.addCallbackBuffer(new byte[bufferSize]);
			camera.addCallbackBuffer(new byte[bufferSize]);

			camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
				public void onPreviewFrame(byte[] data, Camera camera) {
					mRingBuffer.set(data);
				}
			});

			camera.setPreviewDisplay(mCameraSurface.getHolder());
			int result;
			int rotationDegrees = cameraInfo.orientation;
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				result = (cameraInfo.orientation + rotationDegrees) % 360;
				result = (360 - result) % 360;
			} else { 
				result = (cameraInfo.orientation - rotationDegrees + 360) % 360;
			}
			camera.setDisplayOrientation(result);

			mRingBuffer.setCallback(new RingBuffer.Callback() {
				@Override
				public void onBufferRelease(byte[] buffer) {
					if (mCamera != null) {
						mCamera.addCallbackBuffer(buffer);
					}
				}
			});

			camera.startPreview();
			mCamera = camera;

		} catch (Exception e) {
			Log.e("sakalog", "exception", e);
		}
	}

	private void stopCamera() {
		if (mCamera == null) {
			return;
		}
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	public static class RingBuffer {
		private static final int Length = 8;
		private int mSetPos = 0;
		private int mGetPos = 0;
		private byte[][] mArray = null;
		private Callback mCallback = null;
		public RingBuffer() {
			mArray = new byte[Length][];
		}
		public synchronized void setCallback(Callback callback) {
			mCallback = callback;
		}
		public synchronized void set(byte buffer[]) {
			int index = mSetPos % Length;
			byte[] old_buffer = mArray[index];
			mArray[index] = buffer;
			mSetPos++;
			if (old_buffer != null) {
				Log.d("sakalog", "set is faster than get.");
				if (mCallback != null) {
					mCallback.onBufferRelease(old_buffer);
				}
				mGetPos++;
			}
		}
		public synchronized byte[] get() {
			if (mGetPos >= mSetPos) {
				Log.d("sakalog", "set is slower than get.");
				return null;
			}
			int index = mGetPos % Length;
			byte[] buffer = mArray[index];
			mArray[index] = null;
			mGetPos++;
			return buffer;
		}
		public synchronized void release(byte[] buffer) {
			if (mCallback != null && buffer != null) {
				mCallback.onBufferRelease(buffer);
			}
		}
		public abstract static class Callback {
			public abstract void onBufferRelease(byte[] buffer);
		}
	}

	private class EncDecThread extends Thread {

		private boolean mForceInputEOS = false;

		@Override
		public void run() {
			startEncodeDecodeVideo();
		}

		public void finish() {
			// エンコード・デコードを終了
			mForceInputEOS = true;
			try {
				join();
			} catch (Exception e) {
			}
		}

		private void startEncodeDecodeVideo() {
			int width = Width, height = Height;
			int bitRate = Bitrate;
			int frameRate = Framerate;
			String mimeType = "video/avc";
			int threshold = 50;
			int maxerror = 50;
			Surface surface = mDecodeSurface.getHolder().getSurface();
			//Surface surface = null;

			MediaCodec encoder, decoder = null;
			ByteBuffer[] encoderInputBuffers;
			ByteBuffer[] encoderOutputBuffers;
			ByteBuffer[] decoderInputBuffers = null;
			ByteBuffer[] decoderOutputBuffers = null;

			// h.264(video/avc)のCodec(エンコーダ)を検索
			int numCodecs = MediaCodecList.getCodecCount();
			MediaCodecInfo codecInfo = null;
			for (int i = 0; i < numCodecs && codecInfo == null; i++) {
				MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
				Log.d("sakalog", "Codec : " + info.getName());
				if (!info.isEncoder()) {
					Log.d("sakalog", "not encoder");
					continue;
				}
				String[] types = info.getSupportedTypes();
				boolean found = false;
				for (int j = 0; j < types.length && !found; j++) {
					if (types[j].equals(mimeType)) {
						Log.d("sakalog", types[j] + " found!");
						found = true;
					} else {
						Log.d("sakalog", types[j]);
					}
				}
				if (!found)
					continue;
				codecInfo = info;
			}
			if (codecInfo == null) {
				Log.d("sakalog", "Encoder not found");
				return;
			}
			Log.d("sakalog", "Using codec : " + codecInfo.getName() + " supporting " + mimeType);

			// Codec(エンコーダ)への入力となる色フォーマットを決定
			int colorFormat = 0;
			MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
			for (int i = 0; i < capabilities.colorFormats.length /*&& colorFormat == 0*/; i++) {
				int format = capabilities.colorFormats[i];
				Log.d("sakalog", "Color format : " + colorFormatName(format));
				switch (format) {
					case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
					case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
					case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
					case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
					case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
						if (colorFormat == 0)
							colorFormat = format;
						break;
					default:
						break;
				}
			}
			if (colorFormat == 0) {
				Log.d("sakalog", "No supported color format");
				return;
			}
			Log.d("sakalog", "Using color format : " + colorFormatName(colorFormat));

			// このコードの意図は不明
			if (codecInfo.getName().equals("OMX.TI.DUCATI1.VIDEO.H264E")) {
				// This codec doesn't support a width not a multiple of 16, 
				// so round down. 
				width &= ~15;
			}
			int stride = width;
			int sliceHeight = height;
			if (codecInfo.getName().startsWith("OMX.Nvidia.")) {
				stride = (stride + 15)/16*16;
				sliceHeight = (sliceHeight + 15)/16*16;
			}

			// Codec(エンコーダ)のインスタンスを作成
			encoder = MediaCodec.createByCodecName(codecInfo.getName());

			// Codec(エンコーダ)を設定
			MediaFormat outputFormat = MediaFormat.createVideoFormat(mimeType, width, height);
			outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
			outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
			outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
			outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 75);
			outputFormat.setInteger("stride", stride);
			outputFormat.setInteger("slice-height", sliceHeight);
			Log.d("sakalog", "Configuring encoder with output format : " + outputFormat);
			encoder.configure(
					outputFormat,	//the desired format of the output data (encoder).
					null,			//a surface on which to render the output of this decoder. (encoderのためnull指定)
					null,			//a crypto object to facilitate secure decryption of the media data.
					MediaCodec.CONFIGURE_FLAG_ENCODE	//configure the component as an encoder.
					);

			// CodecのInputとOutputのバッファにはgetInputBuffers()とgetOutputBuffers()を使ってアクセスする
			encoder.start();
			encoderInputBuffers = encoder.getInputBuffers();
			encoderOutputBuffers = encoder.getOutputBuffers();

			// UseCameraはカメラ非使用の場合の静止画を生成
			int chromaStride = stride/2;
			int frameSize = stride*sliceHeight + 2*chromaStride*sliceHeight/2;
			byte[] stillImageFrame = new byte[frameSize];
			if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar ||
					colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int Y = (x + y) & 255;
						int Cb = 255*x/width;
						int Cr = 255*y/height;
						stillImageFrame[y*stride + x] = (byte) Y;
						stillImageFrame[stride*sliceHeight + (y/2)*chromaStride + (x/2)] = (byte) Cb;
						stillImageFrame[stride*sliceHeight + chromaStride*(sliceHeight/2) + (y/2)*chromaStride + (x/2)] = (byte) Cr;
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int Y = (x + y) & 255;
						int Cb = 255*x/width;
						int Cr = 255*y/height;
						stillImageFrame[y*stride + x] = (byte) Y;
						stillImageFrame[stride*sliceHeight + 2*(y/2)*chromaStride + 2*(x/2)] = (byte) Cb;
						stillImageFrame[stride*sliceHeight + 2*(y/2)*chromaStride + 2*(x/2) + 1] = (byte) Cr;
					}
				}
			}

			// エンコードとデコードを開始
			// フレームレートは15
			// フレーム間は 1000000/15 = 66666us
			final long kTimeOutUs = 5000;
			MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
			boolean sawInputEOS = false;
			boolean sawOutputEOS = false;
			MediaFormat oformat = null;
			int errors = -1;
			int numInputFrames = 0;
			int maxInputFrames = -1;
			int numOutputFrames = 0;
			float lap, lapavg = 100.0f;
			long lap0 = 0;
			int actualOutputFrame = 0;
			long actualOutputFrameLap = 0;
			actualOutputFrame = 0;
			actualOutputFrameLap = System.currentTimeMillis();
			while (!sawOutputEOS && errors < 0) {

				if (!sawInputEOS) {
					lap = (float)(System.currentTimeMillis() - lap0); // 1frameにかかる処理時間の平均
					lapavg += lap;
					lapavg /= 2.0f;
					float interval = 1000.0f / (float)frameRate;
					if (interval > lapavg) {
						try {
							long sleep = (long)(interval - lapavg);
							//Log.d("sakalog", "lap0 " + lap0 + " lap " + lap + " lapavg " + lapavg + " sleep " + sleep);
							if (sleep > 0) {
								Thread.sleep(sleep);
							}
						} catch (Exception e) {
							Log.d("sakalog", "sleep error.");
						}
					}

					byte[] inputFrame = null;
					boolean release = false;
					if (UseCamera == Camera.CameraInfo.CAMERA_FACING_FRONT || UseCamera == Camera.CameraInfo.CAMERA_FACING_BACK) {
						inputFrame = mRingBuffer.get();
					}
					if (inputFrame == null) {
						inputFrame = stillImageFrame;
					} else {
						release = true;
					}
					if (inputFrame != null) {

						// Codec(エンコーダ)のstart()に続いてdequeueInputBuffer()とdequeueOutputBuffer()を呼び出すことでバッファの所有権がCodecからクライアントに移動する
						// dequeueInputBuffer()とdequeueOutputBuffer()は、InputとOutputのバッファにアクセスするためのインデックスを返す
						// 利用可能なバッファができるまでkTimeOutUsだけ待つ
						// 利用可能なバッファが無い場合は-1が返される
						int inputBufIndex = encoder.dequeueInputBuffer(kTimeOutUs);

						if (inputBufIndex >= 0) {
							//Log.d("sakalog", "encoder input buf index " + inputBufIndex);
							ByteBuffer dstBuf = encoderInputBuffers[inputBufIndex];

							int sampleSize = frameSize;
							long presentationTimeUs = 0;

							// 1フレームぶんのデータをqueueInputBufferを使ってCodec(エンコーダ)へ提示する
							// maxInputFramesフレームに達したらデータの代わりにBUFFER_FLAG_END_OF_STREAMフラグを提示する
							if ((maxInputFrames > 0 && numInputFrames >= maxInputFrames) || mForceInputEOS) {
								Log.d("sakalog", "saw input EOS.");
								sawInputEOS = true;
								sampleSize = 0;
							} else {
								dstBuf.clear();
								dstBuf.put(inputFrame);
								presentationTimeUs = numInputFrames*1000000/frameRate;
								numInputFrames++;
								lap0 = System.currentTimeMillis();
							}

							encoder.queueInputBuffer(
									inputBufIndex,
									0 /* offset */,
									sampleSize,
									presentationTimeUs,
									sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
						}
					}

					if (release) {
						mRingBuffer.release(inputFrame);
					}
				}

				// Codec(エンコーダ)からのOutputバッファ(h.264符号化データ)のインデックスを取得
				// BufferInfo(引数info)にはflags,offset,presentationTimeUs,sizeを取得
				int res = encoder.dequeueOutputBuffer(info, kTimeOutUs);
				if (res >= 0) {
					//Log.d("sakalog", "encoder output buf index " + res);
					int outputBufIndex = res;
					ByteBuffer buf = encoderOutputBuffers[outputBufIndex];

					// BufferInfoに合わせてバッファの読み出し位置と読み出し上限をセット
					buf.position(info.offset);
					buf.limit(info.offset + info.size);

					if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {

						// Codec(エンコーダ)からのOutputバッファが、h.264符号化データではなく、BUFFER_FLAG_CODEC_CONFIGフラグである場合、Codec(デコーダ)のインスタンスをセットアップする
						Log.d("sakalog", "create decoder.");
						decoder = MediaCodec.createDecoderByType(mimeType);
						MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
						format.setByteBuffer("csd-0", buf);
						Log.d("sakalog", "Configuring decoder with input format : " + format);
						decoder.configure(
								format,		//The format of the input data (decoder)
								surface,	//a surface on which to render the output of this decoder.
								null,		//a crypto object to facilitate secure decryption of the media data.
								0			//configure the component as an decoder.
								);
						decoder.start();
						decoderInputBuffers = decoder.getInputBuffers();
						decoderOutputBuffers = decoder.getOutputBuffers();
					} else {

						// Codec(エンコーダ)からのOutputバッファが、h.264符号化データである場合、Codec(デコーダ)へ入力する
						int decIndex = decoder.dequeueInputBuffer(-1);
						//Log.d("sakalog", "decoder input buf index " + decIndex);
						decoderInputBuffers[decIndex].clear();
						decoderInputBuffers[decIndex].put(buf);
						decoder.queueInputBuffer(decIndex, 0, info.size, info.presentationTimeUs, info.flags);
					}

					// Codec(エンコーダ)のOutputバッファ(h.264符号化データ)を処理し終わったらCodec(エンコーダ)へ戻す
					encoder.releaseOutputBuffer(outputBufIndex, false /* render */);
				} else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					encoderOutputBuffers = encoder.getOutputBuffers();

					Log.d("sakalog", "encoder output buffers have changed.");
				} else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					MediaFormat encformat = encoder.getOutputFormat();

					Log.d("sakalog", "encoder output format has changed to " + encformat);
				}

				// Codec(デコーダ)の出力バッファ(rawデータ)のインデックスを取得
				if (decoder == null)
					res = MediaCodec.INFO_TRY_AGAIN_LATER;
				else
					res = decoder.dequeueOutputBuffer(info, kTimeOutUs);

				if (res >= 0) {
					//Log.d("sakalog", "decoder output buf index " + outputBufIndex);
					int outputBufIndex = res;
					ByteBuffer buf = decoderOutputBuffers[outputBufIndex];

					buf.position(info.offset);
					buf.limit(info.offset + info.size);

					if (info.size > 0) {
						//errors = checkFrame(buf, info, oformat, width, height, threshold);
					}

					// 使い終わったOutputバッファはCodec(デコーダ)に戻す
					decoder.releaseOutputBuffer(outputBufIndex, (surface != null) /* render */);

					numOutputFrames++;
					if ((numOutputFrames % (frameRate*3))==0) {
						Log.d("sakalog", "numInputFrames " + numInputFrames + " numOutputFrames " + numOutputFrames + " actualFrameRate " + (float)(numOutputFrames-actualOutputFrame)/(float)(System.currentTimeMillis()-actualOutputFrameLap)*1000.0f + " lapavg " + lapavg);
						actualOutputFrame = numOutputFrames;
						actualOutputFrameLap = System.currentTimeMillis();
					}

					if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						Log.d("sakalog", "saw output EOS.");
						sawOutputEOS = true;
					}
				} else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					decoderOutputBuffers = decoder.getOutputBuffers();

					Log.d("sakalog", "decoder output buffers have changed.");
				} else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					oformat = decoder.getOutputFormat();

					Log.d("sakalog", "decoder output format has changed to " + oformat);
				}

			}

			encoder.stop();
			encoder.release();
			decoder.stop();
			decoder.release();

			Log.d("sakalog", "complete.");
		}
	}
}
