package com.yoonshikhong.sticktogether;

/**
 * Created by Admin on 2/21/2016.
 */
public class AppUtils {

	public static String formatPhoneNumber(String phoneNumber) {
		String formatted = phoneNumber.replaceAll("\\D", "");
		formatted = formatted.substring(formatted.length() - 10);
		return formatted;
	}

}
