package dev.zeith.hpvr;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

public class LogFile
{
	static void setup(Path logs)
			throws IOException
	{
		Files.createDirectories(logs);
		
		var itr = Files.list(logs).filter(f -> f.getFileName().toString().endsWith(".log") && Files.isRegularFile(f)).iterator();
		var now = Instant.now();
		while(itr.hasNext())
		{
			Path p = itr.next();
			if(Duration.between(Files.getLastModifiedTime(p).toInstant(), now).toDays() >= 30L)
				Files.delete(p);
		}
		
		String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		FileOutputStream fileOut = new FileOutputStream(logs.resolve(time + ".log").toFile());
		
		PrintStream out = System.out;
		
		OutputStream dualOut = new OutputStream()
		{
			@Override
			public void write(int b)
					throws IOException
			{
				out.write(b);
				fileOut.write(b);
			}
			
			@Override
			public void flush()
					throws IOException
			{
				out.flush();
				fileOut.flush();
			}
			
			@Override
			public void close()
					throws IOException
			{
				out.close();
				fileOut.close();
			}
		};
		
		PrintStream ps = new PrintStream(dualOut, true);
		System.setOut(ps);
		System.setErr(ps);
	}
}