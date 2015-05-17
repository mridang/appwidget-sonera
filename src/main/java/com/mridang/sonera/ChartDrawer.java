package com.mridang.sonera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * This class is used for drawing charts on a canvas
 */
public class ChartDrawer extends AsyncTask<Float, Void, Uri> {

	/* This is the colour for filling the bars */
	private static final Integer FILL_COLOUR = 0xFF5B0077;
	/* This is the colour for drawing lines */
	private static final Integer LINE_COLOUR = 0xFFDCDCDC;
	/* This is is the height of the rating bar */
	private static final Integer IMAGE_HEIGHT = 40;
	/* This the amount of spacing between the bars */
	private static final Integer IMAGE_WIDTH = 400;
	/* This is the context of the calling activity */
	private final Context ctxContext;

	/**
	 * This is the constructor for the chart drawer
	 * 
	 * @param ctxContext
	 */
	public ChartDrawer(Context ctxContext) {

		this.ctxContext = ctxContext;

	}

	/*
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@SuppressLint("WorldReadableFiles")
	@Override
	protected Uri doInBackground(Float... fltValues) {

		final StringBuilder sbdFilename = new StringBuilder();
		for (int i = 0; i < 1; i++) {
			sbdFilename.append(fltValues[i].toString());
		}

		String strFilename;
		try {

			final MessageDigest md5Digester = MessageDigest.getInstance("MD5");
			md5Digester.reset();
			md5Digester.update(sbdFilename.toString().getBytes());
			final byte[] bytDigests = md5Digester.digest();
			final BigInteger bigInt = new BigInteger(1, bytDigests);
			strFilename = bigInt.toString(16);
			while (strFilename.length() < 32) {
				strFilename = "0" + strFilename;
			}

		} catch (final NoSuchAlgorithmException e) {
			return null;
		}

		final File filFilepath = new File(ctxContext.getCacheDir(), strFilename);
		if (filFilepath.exists()) {
			return Uri.fromFile(filFilepath);
		}

		final Bitmap bmpChart = Bitmap.createBitmap(ChartDrawer.IMAGE_WIDTH, ChartDrawer.IMAGE_HEIGHT, Config.ARGB_8888);
		final Canvas canChart = new Canvas(bmpChart);

		final Paint paiFiller = new Paint();
		paiFiller.setAntiAlias(true);
		paiFiller.setColor(ChartDrawer.FILL_COLOUR);
		paiFiller.setStyle(Paint.Style.FILL);

		final Paint paiEmpty = new Paint();
		paiEmpty.setAntiAlias(true);
		paiEmpty.setColor(ChartDrawer.LINE_COLOUR);
		paiEmpty.setStyle(Paint.Style.FILL);

		canChart.drawRect(0, 0, ChartDrawer.IMAGE_WIDTH * fltValues[0], ChartDrawer.IMAGE_HEIGHT, paiFiller);
		canChart.drawRect(ChartDrawer.IMAGE_WIDTH * fltValues[0], 0, ChartDrawer.IMAGE_WIDTH, ChartDrawer.IMAGE_HEIGHT, paiEmpty);
		canChart.drawBitmap(bmpChart, new Matrix(), null);

		try {

			final FileOutputStream fosThumb = new FileOutputStream(filFilepath);
			bmpChart.compress(Bitmap.CompressFormat.PNG, 100, fosThumb);
			bmpChart.recycle();

		} catch (final FileNotFoundException e) {
			return null;
		}

		makeReadable(filFilepath);
		return Uri.fromFile(filFilepath);

	}

	/**
	 * Makes the file in the cache directory world readable. This currently is not possible through
	 * the SDK so we use reflection to invoke the methods
	 * 
	 * @param ctxContext
	 */	
	protected void makeReadable(File filChart) {

		try {

			Class<?> clsFiles = Class.forName("android.os.FileUtils");
			Method metSet = clsFiles.getMethod("setPermissions", File.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
			metSet.setAccessible(true);
			metSet.invoke(clsFiles, filChart, 644, -1, -1);

		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

}