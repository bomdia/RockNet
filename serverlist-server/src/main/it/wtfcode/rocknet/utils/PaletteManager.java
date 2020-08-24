package it.wtfcode.rocknet.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;

import io.netty.buffer.ByteBuf;
import it.wtfcode.rocknet.Main;

// https://github.com/DragonetMC/DragonProxy/blob/rewrite/proxy/src/main/java/org/dragonet/proxy/util/PaletteManager.java
// Author: lukeeey

public class PaletteManager {

    private ArrayList<RuntimeEntry> entries;

    private ByteBuf cachedPalettes;

    private NbtList<NbtMap> cachedPalette;

    public static final NbtMap BIOMES;

    static {
        InputStream stream = Main.class.getClassLoader().getResourceAsStream("tables/biome_definitions.dat");

        NbtMap biomesTag;

        try (NBTInputStream biomenbtInputStream = NbtUtils.createNetworkReader(stream)){
            biomesTag = (NbtMap) biomenbtInputStream.readTag();
            BIOMES = biomesTag;
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static final NbtMap ENTITY_IDENTIFIERS;

    static {
        InputStream stream = Main.class.getClassLoader().getResourceAsStream("tables/entity_identifiers.dat");

        NbtMap entityTag;

        try (NBTInputStream entitynbtInputStream = NbtUtils.createNetworkReader(stream)){
            entityTag = (NbtMap) entitynbtInputStream.readTag();
            ENTITY_IDENTIFIERS = entityTag;
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static final ItemData[] CREATIVE_ITEMS;

    static {
        /* Load creative items */
        InputStream stream = Main.class.getClassLoader().getResourceAsStream("tables/creative_items.json");

        JsonNode creativeItemEntries;
        try {
            creativeItemEntries = JSON_MAPPER.readTree(stream).get("items");
        } catch (Exception e) {
            throw new AssertionError("Unable to load creative items", e);
        }

        List<ItemData> creativeItems = new ArrayList<>();
        for (JsonNode itemNode : creativeItemEntries) {
            short damage = 0;
            if (itemNode.has("damage")) {
                damage = itemNode.get("damage").numberValue().shortValue();
            }
            if (itemNode.has("nbt_b64")) {
                byte[] bytes = Base64.getDecoder().decode(itemNode.get("nbt_b64").asText());
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                try {
                    NbtMap tag = (NbtMap) NbtUtils.createReaderLE(bais).readTag();
                    creativeItems.add(ItemData.of(itemNode.get("id").asInt(), damage, 1, tag));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                creativeItems.add(ItemData.of(itemNode.get("id").asInt(), damage, 1));
            }
        }
        CREATIVE_ITEMS = creativeItems.toArray(new ItemData[0]);
    }



    public PaletteManager() {
        InputStream stream = Main.class.getClassLoader().getResourceAsStream("tables/runtime_block_states.dat");
        if (stream == null) {
            throw new AssertionError("Unable to locate block state nbt");
        }

        Map<String, Integer> blockIdToIdentifier = new HashMap<>();
        NbtList<NbtMap> tag;

        NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(stream);

        NbtList<NbtMap> blocksTag;
        try {
            tag = (NbtList<NbtMap>) nbtInputStream.readTag();
            nbtInputStream.close();
        } catch (Exception ex) {
            System.out.println("Failed to receive blocks palette");
            throw new AssertionError(ex);
        }

        cachedPalette = tag;
    }

    public NbtList<NbtMap> getCachedPalette() {
		return cachedPalette;
	}

	private static class RuntimeEntry {
        @JsonProperty("name")
        private String name;
        @JsonProperty("id")
        private int id;
        @JsonProperty("data")
        private int data;

        public RuntimeEntry() {}

        public RuntimeEntry(String name, int id, int data) {
            this.id = id;
            this.name = name;
            this.data = data;
        }
    }
}