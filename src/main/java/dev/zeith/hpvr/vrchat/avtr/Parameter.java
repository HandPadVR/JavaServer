package dev.zeith.hpvr.vrchat.avtr;

public record Parameter(String address, ParameterType type)
{
	public enum ParameterType
	{
		Bool,
		Int,
		Float,
		String
	}
	
	@Override
	public String toString()
	{
		return "Parameter[" +
				"address=" + address + ", " +
				"type=" + type + ']';
	}
}
