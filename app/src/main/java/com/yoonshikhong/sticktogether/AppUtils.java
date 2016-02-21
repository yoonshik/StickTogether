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

	public static String formatName(String name) {
		if (!name.contains(" "))
			return name;
		return name.substring(0, name.indexOf(" ") + 2);
	}

}
