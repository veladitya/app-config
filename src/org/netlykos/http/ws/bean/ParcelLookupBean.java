package org.netlykos.http.ws.bean;

public class ParcelLookupBean {

	private volatile boolean isExpired = false;

	public boolean isExpired() {
		return isExpired;
	}

	public void setExpired(boolean isExpired) {
		this.isExpired = isExpired;
	}

}

