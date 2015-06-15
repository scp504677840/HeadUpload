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
	// ����
	private static final int CAMERA_REQUEST_CODE = 1;// ����ͷ������
	private static final int GALLERY_REQUEST_CODE = 2;// ͼ��������
	private static final int CROP_REQUEST_CODE = 3;// �ü�������

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
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// ����ͷ
			startActivityForResult(intent, CAMERA_REQUEST_CODE);
			break;
		case R.id.main_gallery:
			Intent intent2 = new Intent(Intent.ACTION_GET_CONTENT);// ����ѡ���Intent
			intent2.setType("image/*");// ����ѡ������Ϊͼ������
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
			// ͨ��data�Ƿ�Ϊ�����ж��û��Ƿ�����
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
			// ����һ��URI�����淵�ع�����URI
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
		// ��ȡ��SD���е�һ��·��
		File tmpDir = new File(Environment.getExternalStorageDirectory()
				+ "/com.scp");
		// �жϸ�·���Ƿ����
		if (!tmpDir.exists()) {
			tmpDir.mkdir();
		}
		// ����Ҫ������ļ�����
		File img = new File(tmpDir.getAbsolutePath() + "y1.png");
		// ��ȡ���ļ��������
		try {
			FileOutputStream fos = new FileOutputStream(img);
			/**
			 * ��ͼ�������д���������� format:Ҫѹ���ĸ�ʽ quality:ͼƬ������ stream:Ҫд����ļ������
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

	// ͼ��ü�
	private void startImageZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");// �������ݺ�����
		intent.putExtra("crop", true);// �ڿ�����intent�����õ�View�ǿɲü���
		intent.putExtra("aspectX", 1);// �ü��Ŀ�ı���
		intent.putExtra("aspectY", 1);// �ü��ĸߵı���
		intent.putExtra("outputX", 150);// �ü��Ŀ�
		intent.putExtra("outputY", 150);// �ü��ĸ�
		intent.putExtra("return-data", true);// �ü����������ͨ��intent���ص�
		startActivityForResult(intent, CROP_REQUEST_CODE);
	}
	
	//����ͼ������
	private void sendImage(final Bitmap bitmap){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 60, baos);
		byte[]bytes = baos.toByteArray();
		//Base64�ı���
		String imgCode = new String(Base64.encodeToString(bytes, Base64.DEFAULT));
		AsyncHttpClient httpClient = new AsyncHttpClient();
		RequestParams params = new RequestParams();//��������Ҫ����Ĳ���
		params.put("picByte", imgCode);
		httpClient.post("http://192.168.42.217:8080/Metoos/commodityAction!commodityHeader.action", params, new AsyncHttpResponseHandler() {
			
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				MainActivity.this.img.setImageBitmap(bitmap);
				Toast.makeText(MainActivity.this, "�ϴ��ɹ�", 0).show();
			}
			
			@Override
			public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
				Toast.makeText(MainActivity.this, "�ϴ�ʧ��", 0).show();
			}
		});
	}

}
