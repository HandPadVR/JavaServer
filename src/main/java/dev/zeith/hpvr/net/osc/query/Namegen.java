package dev.zeith.hpvr.net.osc.query;

import java.util.Random;

public class Namegen
{
	private static final String K_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final Random RANDOM = new Random();
	
	public static String getRandomChars(int num)
	{
		StringBuilder sb = new StringBuilder(num);
		for(int i = 0; i < num; i++)
		{
			int index = RANDOM.nextInt(K_CHARS.length());
			sb.append(K_CHARS.charAt(index));
		}
		return sb.toString();
	}
}