package dev.zeith.hpvr.vrchat.avtr;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public final class AvatarParameters
		implements Iterable<ParameterIO>
{
	public final List<ParameterIO> value;
	public final int length;
	
	public AvatarParameters(List<ParameterIO> value)
	{
		this.value = value;
		this.length = value.size();
	}
	
	public AvatarParameters filter(Predicate<ParameterIO> filter)
	{
		return new AvatarParameters(value.stream().filter(filter).toList());
	}
	
	public ParameterIO find(Predicate<ParameterIO> filter)
	{
		for(ParameterIO p : value)
			if(filter.test(p))
				return p;
		return null;
	}
	
	public ParameterIO findLast(Predicate<ParameterIO> filter)
	{
		int valueSize = value.size();
		for(int i = valueSize - 1; i >= 0; i--)
		{
			ParameterIO p = value.get(i);
			if(filter.test(p)) return p;
		}
		return null;
	}
	
	@NotNull
	@Override
	public Iterator<ParameterIO> iterator()
	{
		return value.iterator();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == this) return true;
		if(obj == null || obj.getClass() != this.getClass()) return false;
		var that = (AvatarParameters) obj;
		return Objects.equals(this.value, that.value);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(value);
	}
	
	@Override
	public String toString()
	{
		return "AvatarParameters[" +
				"value=" + value + ']';
	}
	
}