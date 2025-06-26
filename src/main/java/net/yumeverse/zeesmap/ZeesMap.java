package net.yumeverse.zeesmap;

import net.fabricmc.api.ModInitializer;

public class ZeesMap implements ModInitializer {
    public static final String MOD_ID = "zees-map";

    @Override
    public void onInitialize() {
        // Server-side initialization
        // WaypointStorage is client-only, so it's initialized in ZeesMapClient
    }
}