package dev.zeith.hpvr.net.osc.query.ported;

import java.util.*;

public class OscQueryRoot
		extends OscQueryNode
{
	private final Map<String, OscQueryNode> nodes;
	
	public OscQueryRoot()
	{
		super("/");
		nodes = new HashMap<>();
		nodes.put("/", this);
	}
	
	public OscQueryNode addNode(OscQueryNode node)
	{
		OscQueryNode parentNode = nodes.get(node.getParentPath());
		if(parentNode == null)
		{
			parentNode = addNode(new OscQueryNode(node.getParentPath()));
		}
		if(parentNode.Contents == null)
		{
			parentNode.Contents = new HashMap<>();
		}
		parentNode.Contents.put(node.getName(), node);
		nodes.put(node.FullPath, node);
		
		return node;
	}
}