package com.mridang.sonera;

import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.mridang.widgets.BaseWidget;
import com.mridang.widgets.SavedSettings;
import com.mridang.widgets.WidgetHelpers;
import com.mridang.widgets.utils.GzippedClient;
import com.mridang.widgets.utils.XML;

/**
 * This class is the provider for the widget and updates the data
 */
public class LauncherWidget extends BaseWidget {

	/*
	 * @see com.mridang.widgets.BaseWidget#fetchContent(android.content.Context, java.lang.Integer,
	 * com.mridang.widgets.SavedSettings)
	 */
	@Override
	public String fetchContent(Context ctxContext, Integer intInstance, SavedSettings objSettings)
			throws Exception {
		
		final DefaultHttpClient dhcClient = GzippedClient.createClient();
		JSONObject jsoData = new JSONObject();

		final HttpPut htpPut = new HttpPut("https://b2c.teliasonera.com/rs/sts/fi/msisdn/authenticate");
		htpPut.setHeader("efs-application-id", "CIPA");
		htpPut.setHeader("efs-login-password", "fNGj95ds");
		htpPut.setHeader("efs-login-user", "sonera");
		htpPut.setHeader("efs-portal-id", "PW");
		htpPut.setHeader("Content-Type", "application/json;charset=UTF-8");

		final JSONObject jsoContent = new JSONObject();
		jsoContent.put("Password", objSettings.get("password"));
		jsoContent.put("Username", objSettings.get("username"));
		htpPut.setEntity(new StringEntity(jsoContent.toString()));

		final HttpResponse resToken = dhcClient.execute(htpPut);

		final Integer intToken = resToken.getStatusLine().getStatusCode();
		if (intToken != HttpStatus.SC_OK) {
			throw new HttpResponseException(intToken, "Server responded with code " + intToken);
		}

		final String strResponse = EntityUtils.toString(resToken.getEntity(), "UTF-8");
		jsoData = XML.toJSONObject(strResponse);

		final HttpGet htpGet = new HttpGet("https://b2c.teliasonera.com/rs/b2css/fi/usageinfo");
		htpGet.setHeader("efs-application-id", "CIPA");
		htpGet.setHeader("efs-login-password", "fNGj95ds");
		htpGet.setHeader("efs-login-user", "sonera");
		htpGet.setHeader("efs-portal-id", "PW");
		htpGet.setHeader("STS-MSISDN", jsoData.getJSONObject("ns2:Ticket").getString("Username"));
		htpGet.setHeader("Cookie", "STSSESSION=" + jsoData.getJSONObject("ns2:Ticket").getString("Value"));
		final HttpResponse resUsage = dhcClient.execute(htpGet);

		final Integer intUsage = resUsage.getStatusLine().getStatusCode();
		if (intUsage != HttpStatus.SC_OK) {
			throw new HttpResponseException(intUsage, "Server responded with code " + intUsage);
		}

		final String strUsage = EntityUtils.toString(resUsage.getEntity(), "UTF-8");
		jsoData = XML.toJSONObject(strUsage);
		return jsoData.toString(2);

	}

	/*
	 * @see com.mridang.widgets.BaseWidget#getIcon()
	 */
	@Override
	public Integer getIcon() {

		return R.drawable.ic_notification;

	}

	/*
	 * @see com.mridang.widgets.BaseWidget#getKlass()
	 */
	@Override
	protected Class<?> getKlass() {

		return getClass();

	}

	/*
	 * @see com.mridang.BaseWidget#getToken()
	 */
	@Override
	public String getToken() {

		return "a1b2c3d4";

	}

	/*
	 * @see com.mridang.widgets.BaseWidget#updateWidget(android.content.Context, java.lang.Integer,
	 * com.mridang.widgets.SavedSettings, java.lang.String)
	 */
	@Override
	public void updateWidget(Context ctxContext, Integer intInstance, SavedSettings objSettings, String strContent)
			throws Exception {

		final RemoteViews remView = new RemoteViews(ctxContext.getPackageName(), R.layout.widget);
		final JSONObject jsoData = new JSONObject(strContent);
		final ChartDrawer objCharter = new ChartDrawer(ctxContext);

		remView.setTextViewText(R.id.plan_name, String.valueOf(jsoData.getJSONObject("ns2:UsageInfo").getJSONObject("Balance").getInt("Amount") * -1.00d) + " \u20ac");
		remView.setTextViewText(R.id.phone_number, objSettings.get("username"));

		JSONArray jsoQuotas = jsoData.getJSONObject("ns2:UsageInfo").getJSONObject("Balance").getJSONObject("QuotaUsage").getJSONArray("Quota");

		Integer intTexts = jsoQuotas.getJSONObject(1).getInt("Used");
		Float fltTexts = (float) ( (float) intTexts / jsoQuotas.getJSONObject(1).getInt("Limit"));
		ChartDrawer objTexts = new ChartDrawer(ctxContext);
		objTexts.execute(fltTexts);
		remView.setImageViewUri(R.id.texts_usage, objTexts.get());
		remView.setTextViewText(R.id.texts_used, jsoQuotas.getJSONObject(1).getString("Used") + " SMS");
		remView.setTextViewText(R.id.texts_total, jsoQuotas.getJSONObject(1).getString("Limit") + " SMS");

		Integer intCalls = jsoQuotas.getJSONObject(0).getInt("Used");
		Float fltCalls = (float) ((float) intCalls / jsoQuotas.getJSONObject(0).getInt("Limit"));
		ChartDrawer objCalls = new ChartDrawer(ctxContext);
		objCalls.execute(fltCalls);
		remView.setImageViewUri(R.id.calls_usage, objCalls.get());
		remView.setTextViewText(R.id.calls_used, jsoQuotas.getJSONObject(0).getString("Used") + " MIN");
		remView.setTextViewText(R.id.calls_total, jsoQuotas.getJSONObject(0).getString("Limit") + " MIN");

		Long lngData = jsoQuotas.getJSONObject(2).getLong("Used") / 1048576L;
		Long lngData1 = (long) (lngData / (jsoQuotas.getJSONObject(2).getLong("Limit") / 1048576L));
		ChartDrawer objData = new ChartDrawer(ctxContext);
		objData.execute(lngData1.floatValue());
		remView.setImageViewUri(R.id.data_usage, objData.get());
		remView.setTextViewText(R.id.data_used, FileUtils.byteCountToDisplaySize(jsoQuotas.getJSONObject(2).getLong("Used")));
		remView.setTextViewText(R.id.data_total, FileUtils.byteCountToDisplaySize(jsoQuotas.getJSONObject(2).getLong("Limit")));

		final String strWebpage = "http://www.sonera.fi/Kirjaudu/";
		final PendingIntent pitOptions = WidgetHelpers.getIntent(ctxContext, WidgetSettings.class, intInstance);
		final PendingIntent pitWebpage = WidgetHelpers.getIntent(ctxContext, strWebpage);
		remView.setTextViewText(R.id.last_update, DateFormat.format("kk:mm", new Date()));
		remView.setOnClickPendingIntent(R.id.settings_button, pitOptions);
		remView.setOnClickPendingIntent(R.id.widget_layout, pitWebpage);

		AppWidgetManager.getInstance(ctxContext).updateAppWidget(intInstance, remView);

	}

}