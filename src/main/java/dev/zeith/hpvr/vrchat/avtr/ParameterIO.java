package dev.zeith.hpvr.vrchat.avtr;

import java.util.Objects;

public final class ParameterIO
{
	public final String name;
	public final Parameter input;
	public final Parameter output;
	
	public ParameterIO(
			String name,
			Parameter input,
			Parameter output
	)
	{
		this.name = name;
		this.input = input;
		this.output = output;
	}
	
	public boolean canUpdate()
	{
		return input != null;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == this) return true;
		if(obj == null || obj.getClass() != this.getClass()) return false;
		var that = (ParameterIO) obj;
		return Objects.equals(this.name, that.name) &&
				Objects.equals(this.input, that.input) &&
				Objects.equals(this.output, that.output);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(name, input, output);
	}
	
	@Override
	public String toString()
	{
		return "ParameterData[" +
				"name=" + name + ", " +
				"input=" + input + ", " +
				"output=" + output + ']';
	}
	
}
