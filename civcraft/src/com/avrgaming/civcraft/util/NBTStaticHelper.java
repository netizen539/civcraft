package com.avrgaming.civcraft.util;

public class NBTStaticHelper {

	/* From the decompiled 1.7 jar...
	 *   protected static NBTBase createTag(byte paramByte)
  {
    switch (paramByte)
    {
    case 0: 
      return new NBTTagEnd();
    case 1: 
      return new NBTTagByte();
    case 2: 
      return new NBTTagShort();
    case 3: 
      return new NBTTagInt();
    case 4: 
      return new NBTTagLong();
    case 5: 
      return new NBTTagFloat();
    case 6: 
      return new NBTTagDouble();
    case 7: 
      return new NBTTagByteArray();
    case 11: 
      return new NBTTagIntArray();
    case 8: 
      return new NBTTagString();
    case 9: 
      return new NBTTagList();
    case 10: 
      return new NBTTagCompound();
    }
    return null;
  }
	 */
	
	public static final int TAG_LIST = 9;
	public static final int TAG_COMPOUND = 10;
	public static final int TAG_DOUBLE = 6;
	public static final int TAG_FLOAT = 5;
	public static final int TAG_STRING = 8;
}
