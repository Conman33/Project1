package dev.example.assetmap;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class AssetMapAddon extends MeteorAddon {
    @Override
    public void onInitialize() {
        Modules.get().add(new ClusterESP());
    }

    @Override
    public String getPackage() {
        return "dev.example.assetmap";
    }
}
