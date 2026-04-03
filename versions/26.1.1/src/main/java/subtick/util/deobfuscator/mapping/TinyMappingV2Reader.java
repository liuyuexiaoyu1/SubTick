package subtick.util.deobfuscator.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

/**
 * Copy from Carpet-TIS-Addition
 * 此文件以LGPL-3.0协议开源
 */

public class TinyMappingV2Reader implements MappingReader {
	@Override
	public Map<String, String> readMapping(BufferedReader mappingReader) throws IOException
	{
		// unused in unobfuscated version
		return Map.of();
	}
}
