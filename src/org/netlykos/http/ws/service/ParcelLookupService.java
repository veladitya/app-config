package org.netlykos.http.ws.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.netlykos.http.ws.bean.ParcelLookupBean;

public class ParcelLookupService {

	private ScheduledThreadPoolExecutor  scheduledExecutorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(20);
	private Map<String, String> imageIdToSeDataMap = new HashMap<>();

	public ScheduledFuture<?> process(ParcelLookupBean query) {
		ParcelLookupBean parcelLookupBean = new ParcelLookupBean();
		return scheduledExecutorService.schedule(() -> {
			expireCallback(parcelLookupBean);
		}, 60000, TimeUnit.MILLISECONDS);		
	}

	public void expireCallback(ParcelLookupBean parcelLookupBean) {
		System.out.println(parcelLookupBean.isExpired());
	}

	public static void main(String args[]) {
		ParcelLookupService parcelLookupService = new ParcelLookupService();
		parcelLookupService.imageIdToSeDataMap.put("data", "data string");
		ParcelLookupBean parcelLookupBean = new ParcelLookupBean();
		

		ScheduledFuture<?> scheduledFuture  = parcelLookupService.process(parcelLookupBean); 
		//scheduledFuture.cancel(true);
		// Adding some delay
		try {
			parcelLookupService.scheduledExecutorService.awaitTermination(60000L, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		parcelLookupService.scheduledExecutorService.shutdown();
		System.out.println("Completed all threads");
	}
}
