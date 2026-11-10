/* 
 * QR Code generator library (Java)
 * 
 * Copyright (c) Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/qr-code-generator-library
 */

package com.hyper.choosebrowsernew.util.qr;

public class DataTooLongException extends IllegalArgumentException {
	
	public DataTooLongException() {}
	
	public DataTooLongException(String msg) {
		super(msg);
	}
}
