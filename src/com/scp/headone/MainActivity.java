package com.scp.headone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private Button camera;
	private Button gallery;
	private ImageView img;
	// 其他
	private static final int CAMERA_REQUEST_CODE = 1;// 摄像头请求码
	private static final int GALLERY_REQUEST_CODE = 2;// 图库请求码
	private static final int CROP_REQUEST_CODE = 3;// 裁剪请求码

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_main);
		initView();
		initEvent();
	}

	private void initView() {
		camera = (Button) findViewById(R.id.main_camera);
		gallery = (Button) findViewById(R.id.main_gallery);
		img = (ImageView) findViewById(R.id.main_img);
	}

	private void initEvent() {
		camera.setOnClickListener(this);
		gallery.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.main_camera:
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 摄像头
			startActivityForResult(intent, CAMERA_REQUEST_CODE);
			break;
		case R.id.main_gallery:
			Intent intent2 = new Intent(Intent.ACTION_GET_CONTENT);// 内容选择的Intent
			intent2.setType("image/*");// 内容选择类型为图像类型
			startActivityForResult(intent2, GALLERY_REQUEST_CODE);
			break;

		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CAMERA_REQUEST_CODE:
			// 通过data是否为空来判断用户是否拍照
			if (data == null) {
				return;
			}
			Bundle extras = data.getExtras();
			if (extras != null) {
				Bitmap bitmap = extras.getParcelable("data");
				Uri bmUri = saveBitmap(bitmap);
				startImageZoom(bmUri);
			}
			break;
		case GALLERY_REQUEST_CODE:
			if (data == null) {
				return;
			}
			// 定义一个URI来保存返回过来的URI
			Uri uri;
			uri = data.getData();
			Uri fileUri = convertUri(uri);
			startImageZoom(fileUri);
			break;
		case CROP_REQUEST_CODE:
			if (data == null) {
				return;
			}
			Bundle bundle = data.getExtras();
			Bitmap bitmap = bundle.getParcelable("data");
			sendImage(bitmap);
			break;

		default:
			break;
		}
	}

	private Uri convertUri(Uri uri) {
		InputStream is = null;
		try {
			is = getContentResolver().openInputStream(uri);
			Bitmap bitmap = BitmapFactory.decodeStream(is);
			is.close();
			return saveBitmap(bitmap);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Uri saveBitmap(Bitmap bitmap) {
		// 获取到SD卡中的一个路径
		File tmpDir = new File(Environment.getExternalStorageDirectory()
				+ "/com.scp");
		// 判断该路径是否存在
		if (!tmpDir.exists()) {
			tmpDir.mkdir();
		}
		// 创建要保存的文件对象
		File img = new File(tmpDir.getAbsolutePath() + "y1.png");
		// 获取该文件的输出流
		try {
			FileOutputStream fos = new FileOutputStream(img);
			/**
			 * 将图像的数据写入该输出流中 format:要压缩的格式 quality:图片的质量 stream:要写入的文件输出流
			 */
			bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);
			fos.flush();
			fos.close();
			return Uri.fromFile(img);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// 图像裁剪
	private void startImageZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");// 设置数据和类型
		intent.putExtra("crop", true);// 在开启的intent中设置的View是可裁剪的
		intent.putExtra("aspectX", 1);// 裁剪的宽的比例
		intent.putExtra("aspectY", 1);// 裁剪的高的比例
		intent.putExtra("outputX", 150);// 裁剪的宽
		intent.putExtra("outputY", 150);// 裁剪的高
		intent.putExtra("return-data", true);// 裁剪后的数据是通过intent返回的
		startActivityForResult(intent, CROP_REQUEST_CODE);
	}
	
	//发送图像数据
	private void sendImage(final Bitmap bitmap){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 60, baos);
		byte[]bytes = baos.toByteArray();
		//Base64的编码
		String imgCode = new String(Base64.encodeToString(bytes, Base64.DEFAULT));
		AsyncHttpClient httpClient = new AsyncHttpClient();
		RequestParams params = new RequestParams();//保存我们要传输的参数
		params.put("picByte", imgCode);
		httpClient.post("http://192.168.42.217:8080/Metoos/commodityAction!commodityHeader.action", params, new AsyncHttpResponseHandler() {
			
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				MainActivity.this.img.setImageBitmap(bitmap);
				Toast.makeText(MainActivity.this, "上传成功", 0).show();
			}
			
			@Override
			public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
				Toast.makeText(MainActivity.this, "上传失败", 0).show();
			}
		});
	}

}
